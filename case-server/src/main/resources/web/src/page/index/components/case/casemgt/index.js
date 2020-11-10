/* eslint-disable */
import React from 'react';
import PropTypes from 'prop-types';
import { Row, Button, Col, message, Tooltip } from 'antd';
import './index.scss';
import request from '../../../../../util/request';
import getQueryString from '../../../../../util/getCookies';
const getCookies = getQueryString.getCookie;
import moment from 'moment';
// import Link from 'umi/link';
import AgileTCEditor from 'react-agiletc-editor';
/* global staffNamePY */
export default class CaseMgt extends React.Component {
  static propTypes = {
    params: PropTypes.any,
    form: PropTypes.any,
    productId: PropTypes.any,
    updateCallBack: PropTypes.any,
    activeProductObj: PropTypes.any,
  };
  constructor(props) {
    super(props);
    this.state = {
      modaltitle: '',
      visibleStatus: false,
      visible: false,
      title: '',
      caseContent: '',
      id: 0,
      productId: 0,
      recordDetail: null,
      casedetail: null,
      requirementObj: [],
    };
  }
  componentDidMount() {
    const { iscore } = this.props.match.params;

    if (iscore === '3') {
      this.getContentById();
    } else {
      this.getCaseById();
    }
  }

  componentWillMount() {
    // 拦截判断是否离开当前页面
    window.addEventListener('beforeunload', this.beforeunload);
  }
  componentWillUnmount() {
    // 销毁拦截判断是否离开当前页面
    window.removeEventListener('beforeunload', this.beforeunload);
  }
  ///case/getRequirement

  getRequirementsById = (requirementIds) => {
    // request(`/${this.props.oeApiPrefix}/business-lines/requirements`, {
    //   method: 'GET',
    //   params: { requirementIds: requirementIds },
    // }).then(res => {
    //   this.setState({ requirementObj: res, loading: false });
    // });
  };

  getCaseById = () => {
    let url = ` /case/getCaseById`;

    if (this.props.type === 'oe') {
      url = `/${this.props.doneApiPrefix}/case/getCaseById`;
    }
    request(url, {
      method: 'GET',
      params: { id: this.props.match.params.caseId },
    }).then((res) => {
      if (res.code == 200) {
        this.setState(
          {
            casedetail: res.data,
          },
          () => {
            this.state.casedetail.requirementId &&
              this.getRequirementsById(this.state.casedetail.requirementId);
          },
        );
      } else {
        message.error(res.msg);
      }
    });
  };

  ///execRecord/getContentById
  getContentById = () => {
    let url = `/execRecord/getContentById`;

    if (this.props.type === 'oe') {
      url = `/${this.props.doneApiPrefix}/execRecord/getContentById`;
    }
    request(url, {
      method: 'GET',
      params: { id: this.props.match.params.itemid },
    }).then((res) => {
      if (res.code == 200) {
        this.setState({ recordDetail: res.data });
      } else {
        message.error(res.msg);
      }
    });
  };

  //保存用例
  updateCase = () => {
    let recordId =
      this.props.match.params.itemid == 'undefined'
        ? undefined
        : this.props.match.params.itemid;

    const param = {
      id: this.props.match.params.caseId,
      title: '更新内容，实际不会保存title',
      groupId: recordId,
      modifier: getCookies('username'),
      caseContent: JSON.stringify(this.editorNode.getAllData()),
    };
    let url = `/case/update`;

    if (this.props.type === 'oe') {
      url = `/${this.props.doneApiPrefix}/case/update`;
    }

    request(url, { method: 'POST', body: param }).then((res) => {
      if (res.code == 200) {
        message.success('保存内容成功');
      } else {
        message.error(res.msg);
      }
    });
  };

  //清除执行记录
  clearRecord = () => {
    const params = {
      id: this.props.match.params.itemid,
      modifier: getCookies('username'),
      productLineId: this.props.match.params.product_id
        ? this.props.match.params.product_id
        : '',
    };

    let url = `/execRecord/clearResult`;

    if (this.props.type === 'oe') {
      url = `/${this.props.doneApiPrefix}/execRecord/clearResult`;
    }
    request(url, { method: 'POST', body: params }).then((res) => {
      if (res.code == 200) {
        message.success('清除执行记录成功');
        this.editorNode.setEditerData(JSON.parse(res.data.caseContent));
      } else {
        message.error(res.msg);
      }
    });
  };

  render() {
    //this.props.match.params.iscore  0:需求case  3:执行记录详情
    const { type, match, baseUrl } = this.props;
    const { iscore, caseId, itemid } = match.params;
    const user = getCookies('username');
    const { recordDetail, casedetail, requirementObj } = this.state;
    let readOnly = false;
    let progressShow = false;
    if (iscore === '0' || iscore === '1') {
      readOnly = false;
      progressShow = false;
    } else {
      readOnly = true;
      progressShow = true;
    }
    return (
      <div style={{ position: 'relative', minHeight: '80vh' }}>
        <div
          style={{
            padding: 12,
            background: '#fff',
          }}
        >
          {(recordDetail && (
            <Row>
              <Col>
                <Row className="case-title m-b-18">
                  <Col span="2">测试记录:</Col>

                  <Col span="22">{recordDetail.title}</Col>
                </Row>
              </Col>

              <Col span="6" className="description-case elipsis-case">
                <Tooltip
                  title={recordDetail.description}
                  placement="bottomLeft"
                >
                  {recordDetail.description}
                </Tooltip>
              </Col>
              <Col span="1"></Col>

              <Col span="2" className="font-size-12">
                通过率: {recordDetail.passRate.toFixed(2) + '%'}
              </Col>
              <Col span="2" className="font-size-12">
                {' '}
                已测: {recordDetail.passCount + '/' + recordDetail.totalCount}
              </Col>
              <Col
                span="4"
                style={{ textAlign: 'center' }}
                className="progress"
              >
                <div>
                  {(
                    <Tooltip
                      title={`通过:${recordDetail.successCount} (${(
                        (recordDetail.successCount / recordDetail.totalCount) *
                        100
                      ).toFixed(2)}%)`}
                      className="font-size-12"
                    >
                      <div
                        className="div-wrap"
                        style={{
                          width: `${
                            (recordDetail.successCount /
                              recordDetail.totalCount) *
                            100
                          }%`,
                          backgroundColor: '#61C663',
                        }}
                      >
                        <span></span>
                      </div>
                    </Tooltip>
                  ) || null}
                  {(recordDetail.blockCount > 0 && (
                    <Tooltip
                      title={`阻塞:${recordDetail.blockCount} (${(
                        (recordDetail.blockCount / recordDetail.totalCount) *
                        100
                      ).toFixed(2)}%)`}
                      className="font-size-12"
                    >
                      <div
                        className="div-wrap"
                        style={{
                          width: `${
                            (recordDetail.blockCount /
                              recordDetail.totalCount) *
                            100
                          }%`,
                          backgroundColor: '#85A1D6',
                        }}
                      >
                        <span></span>
                      </div>
                    </Tooltip>
                  )) ||
                    null}
                  {(recordDetail.bugNum > 0 && (
                    <Tooltip
                      title={`失败:${recordDetail.bugNum} (${(
                        (recordDetail.bugNum / recordDetail.totalCount) *
                        100
                      ).toFixed(2)}%)`}
                    >
                      <div
                        className="div-wrap"
                        style={{
                          width: `${
                            (recordDetail.bugNum / recordDetail.totalCount) *
                            100
                          }%`,
                          backgroundColor: '#FF7575',
                        }}
                      >
                        <span></span>
                      </div>
                    </Tooltip>
                  )) ||
                    null}
                  {(recordDetail.totalCount - recordDetail.passCount > 0 && (
                    <Tooltip
                      title={`未执行:${
                        recordDetail.totalCount - recordDetail.passCount
                      } (${(
                        ((recordDetail.totalCount - recordDetail.passCount) /
                          recordDetail.totalCount) *
                        100
                      ).toFixed(2)}%)`}
                    >
                      <div
                        className="div-wrap"
                        style={{
                          width: `${
                            ((recordDetail.totalCount -
                              recordDetail.passCount) /
                              recordDetail.totalCount) *
                            100
                          }%`,
                          backgroundColor: '#EDF0FA',
                        }}
                      >
                        <span></span>
                      </div>
                    </Tooltip>
                  )) ||
                    null}
                </div>
              </Col>
              <Col span="1"></Col>
              <Col span="2" className="font-size-12">
                计划周期:
              </Col>
              <Col span="4" className="font-size-12">
                {recordDetail.expectStartTime
                  ? moment(recordDetail.expectStartTime).format('YYYY/MM/DD')
                  : null}
                -{' '}
                {recordDetail.expectEndTime
                  ? moment(recordDetail.expectEndTime).format('YYYY/MM/DD')
                  : null}
              </Col>
            </Row>
          )) ||
            null}

          {(casedetail && (
            <Row>
              <Col className="case-title">
                <Row className="m-b-18">
                  <Col span="2">用例集名称:</Col>

                  <Col span="22">{casedetail.title}</Col>
                </Row>
              </Col>

              <Col span="6" className="description-case elipsis-case">
                <Tooltip title={casedetail.description} placement="topLeft">
                  {casedetail.description}
                </Tooltip>
              </Col>
              <Col span="1"></Col>
              <Col span="2" className="font-size-12">
                关联需求:
              </Col>
              <Col span="14" className="font-size-12">
                {casedetail ? casedetail.requirementId : ''}
                {/* {(requirementObj &&
                  requirementObj.map((item, index) => {
                    let titleStr = item.title;
                    if (index !== 0) {
                      titleStr = ' , ' + item.title;
                    }

                    return (
                      <Link
                        to={`${baseUrl}/${item.requirementId}`}
                        key={item.requirementId}
                        target="_blank"
                      >
                        {titleStr}
                      </Link>
                    );
                  })) ||
                  null} */}
              </Col>
            </Row>
          )) ||
            null}
          <div
            style={{
              display: 'inline-block',
              position: 'fixed',
              bottom: '30px',
              right: '20px',
              zIndex: 9,
            }}
          >
            {iscore != 2 && (
              <Button type="primary" onClick={this.updateCase}>
                保存
              </Button>
            )}
            <span> &nbsp; &nbsp;</span>
            {iscore == 3 && (
              <Button type="primary" onClick={this.clearRecord}>
                清除执行记录
              </Button>
            )}
          </div>
          <AgileTCEditor
            ref={(editorNode) => (this.editorNode = editorNode)}
            tags={['前置条件', '执行步骤', '预期结果']}
            progressShow={progressShow}
            readOnly={readOnly}
            editorStyle={{ height: 'calc(100vh - 200px)' }}
            toolbar={{
              image: false,
              theme: ['classic-compact', 'fresh-blue', 'fresh-green-compat'],
              template: ['default', 'right', 'fish-bone'],
            }}
            baseUrl="/"
            uploadUrl="/api/projmgr/common/uploadAttachment"
            wsUrl={`ws://${window.location.host}/api/case/${caseId}/${itemid}/${iscore}/${user}`}
            onSave={
              Number(iscore) !== 2
                ? () => {
                    message.loading('保存中......', 1);
                    this.updateCase();
                  }
                : null
            }
          />
        </div>
      </div>
    );
  }
}

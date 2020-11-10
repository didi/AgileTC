/* eslint-disable */
import React from 'react';
import PropTypes from 'prop-types';
import request from '@/utils/axios';
import { Row, Button, Col, Icon, Form, message } from 'antd';
import './index.scss';
import moment from 'moment';
moment.locale('zh-cn');
import _ from 'lodash';
import CaseModal from './caseModal.js';
import List from './list.js';
import Filter from './filter.js';
import OeFilter from './oefilter';

class CaseLists extends React.Component {
  static propTypes = {
    form: PropTypes.any,
    productId: PropTypes.any,
    updateCallBack: PropTypes.any,
    users: PropTypes.any,
  };
  constructor(props) {
    super(props);
    this.state = {
      list: [],
      total: 0, // 数据条数
      record: {},
      title: '',
      visible: false,
      iterationList: [], // 需求列表
      showFilterBox: false, // 展示筛选框
      productMember: [], // 所有人
      currCase: null, // 当前选中case
      showAddRecord: false, // 展开添加记录弹框
      envList: [], // 执行记录环境列表
      options: { projectLs: [], requirementLs: [] },
      requirement: null,
      project: null,
      projectLs: [],
      filterStatus: 'filter-hide',
      filterVisble: false,
      loading: true,
      current: 1,
      product_id: '',
    };
  }
  componentDidMount() {
    this.setState(
      {
        product_id: this.props.match.params.product_id,
      },
      () => {
        this.getProductMumber();
        this.getCaseList(1, '', '', '', []);
      },
    );

    if (this.props.type !== 'oe') {
      this.getRequirementList();
      this.getProjectList();
    }
  }
  componentWillReceiveProps(nextProps) {
    if (
      this.props.match.params.product_id != nextProps.match.params.product_id
    ) {
      this.setState(
        {
          product_id: nextProps.match.params.product_id,
        },
        () => {
          this.getCaseList(1, '', '', '', []);
          this.getProductMumber();
        },
      );
    }
  }
  getCaseList = (
    current,
    nameFilter,
    createrFilter,
    iterationFilter,
    choiseDate,
  ) => {
    let { type } = this.props;

    request(`${this.props.doneApiPrefix}/case/list`, {
      method: 'GET',
      params: {
        pageSize: 10,
        pageNum: current,
        productLineId: this.state.product_id,
        case_type: 0, // eslint-disable-line
        title: nameFilter || '',
        creator: createrFilter || '',
        channel: this.props.type === 'oe' ? 1 : 0,
        requirement_id: iterationFilter || '', // eslint-disable-line
        beginTime: choiseDate.length > 0 ? `${choiseDate[0]} 00:00:00` : '',
        endTime: choiseDate.length > 0 ? `${choiseDate[1]}  23:59:59` : '',
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState({
          list: res.data.data,
          total: res.data.total,
          current,
          nameFilter,
          createrFilter,
          iterationFilter,
          choiseDate,
        });
      } else {
        message.error(res.msg);
      }
      this.setState({ loading: false });
      return null;
    });
  };

  getProjectList = () => {
    let productId =
      this.props.match.params === undefined
        ? this.state.activeProductObj.id
        : this.props.match.params.product_id;
    request('/projmgr/iteration/getProductIteration', {
      method: 'GET',
      params: {
        productId: productId,
      },
    }).then(res => {
      if (res.success > 0) {
        let projectLs = res.data.ing;
        projectLs.push({ id: 0, name: '零散需求' });
        this.setState(
          {
            projectLs: projectLs,
          },
          () => this.initCaseModalInfo(),
        );
      }
    });
  };
  getRequirementList = () => {
    let productId = this.props.match.params.product_id;
    if (!productId) return;

    request(`/projmgr/requirement/getGroupedRequirementsByProductId`, {
      method: 'GET',
      params: { productId: productId },
    }).then(res => {
      if (res.success > 0) {
        let { outPoll } = res.data;
        this.setState({ requirementLs: outPoll }, () =>
          this.initCaseModalInfo(),
        );
      } else {
        //
      }
    });
  };

  initCaseModalInfo = () => {
    let { projectLs, requirementLs } = this.state;
    let project = projectLs.length > 0 ? projectLs[0] : null;
    let requirement = null;
    if (project) {
      requirement = _.find(projectLs, item => {
        return item.iterationId === project.id;
      });
    }
    this.setState({
      options: {
        project: project,
        requirement: requirement,
        projectLs: projectLs,
        requirementLs: requirementLs,
      },
    });
  };
  getProductMumber = () => {
    let { type } = this.props;
    let url = '/case/listCreators';
    if (type === 'oe') {
      url = `${this.props.doneApiPrefix}/case/listCreators`;
    }
    request(url, {
      method: 'GET',
      params: { productLineId: this.state.product_id, case_type: 0 },
    }).then(res => {
      if (res.code === 200) {
        this.setState({
          productMember: res.data,
        });
      }
    });
  };
  handleTask = (val, record, project, requirement, current) => {
    this.setState(
      {
        visible: true,
        title: val,
        currCase: record,
        project,
        requirement,
        current,
      },
      () => {
        this.props.form.resetFields();
      },
    );
  };
  onShowFilterBoxClick = () => {
    let showFilterBox = !this.state.showFilterBox;
    this.setState({
      showFilterBox,
      iterationFilter: '',
      nameFilter: '',
      choiseDate: [],
      createrFilter: '',
    });
  };
  onClose = vis => {
    this.setState({ visible: vis });
  };

  filterHandler = () => {
    this.setState({ filterStatus: 'filter-show', filterVisble: true });
  };

  closeFilter = () => {
    this.setState({ filterStatus: 'filter-hide', filterVisble: false });
  };

  render() {
    const {
      project,
      requirement,
      list,
      total,
      productMember,
      filterVisble,
      filterStatus,
      nameFilter,
      createrFilter,
      iterationFilter,
      choiseDate,
    } = this.state;
    const { product_id } = this.props.match.params;
    return (
      <div className="min-hig-content">
        <div className="site-drawer-render-in-current-wrapper">
          <Row className="m-b-10">
            <Col>
              {(this.props.type !== 'oe' && (
                <Col span={18} className="text-left">
                  <Button
                    type="primary"
                    className="m-l-10"
                    onClick={this.onShowFilterBoxClick}
                  >
                    {(this.state.showFilterBox && (
                      <span>
                        <Icon type="minus" />
                        收起
                      </span>
                    )) || (
                      <span>
                        <Icon type="filter" /> 筛选
                      </span>
                    )}
                  </Button>
                </Col>
              )) || (
                <Col span={18}>
                  <Col span={18}>
                    <div style={{ margin: '10px' }}>
                      快速筛选：<a>全部({total})</a>
                    </div>
                  </Col>
                </Col>
              )}
              <Col xs={6} className="text-right">
                {(this.props.type === 'oe' && (
                  <Button className="m-l-10" onClick={this.filterHandler}>
                    <Icon type="filter" /> 筛选
                  </Button>
                )) ||
                  null}
                &nbsp;&nbsp;&nbsp;
                <Button
                  type="primary"
                  onClick={text => {
                    this.handleTask('add');
                    this.setState({
                      currCase: null,
                      visible: true,
                      project: null,
                      requirement: null,
                    });
                  }}
                >
                  <Icon type="plus" /> 新建用例集
                </Button>
              </Col>
            </Col>
          </Row>
          <hr
            style={{ border: '0', backgroundColor: '#e8e8e8', height: '1px' }}
          />
          {this.state.showFilterBox && (
            <Filter
              getCaseList={this.getCaseList}
              productMember={productMember}
            />
          )}
          <List
            productId={product_id}
            options={this.state.options}
            list={list}
            total={total}
            handleTask={this.handleTask}
            getCaseList={this.getCaseList}
            type={this.props.type}
            loading={this.state.loading}
            baseUrl={this.props.baseUrl}
            oeApiPrefix={this.props.oeApiPrefix}
            doneApiPrefix={this.props.doneApiPrefix}
            current={this.state.current}
            nameFilter={nameFilter}
            createrFilter={createrFilter}
            iterationFilter={iterationFilter}
            choiseDate={choiseDate}
          ></List>

          {(this.props.type === 'oe' && filterVisble && (
            <OeFilter
              onCancel={this.closeFilter}
              getCaseList={this.getCaseList}
              productMember={productMember}
              filterStatus={filterStatus}
              closeFilter={this.closeFilter}
              visible={filterVisble}
              oeApiPrefix={this.props.oeApiPrefix}
              productId={product_id}
            />
          )) ||
            null}
        </div>

        {this.state.visible && (
          <CaseModal
            productId={product_id}
            data={this.state.currCase}
            title={this.state.title}
            project={project}
            requirement={requirement}
            options={this.state.options}
            show={this.state.visible}
            onClose={this.onClose}
            oeApiPrefix={this.props.oeApiPrefix}
            doneApiPrefix={this.props.doneApiPrefix}
            baseUrl={this.props.baseUrl}
            onUpdate={() => {
              this.getCaseList(this.state.current || 1, '', '', '', []);
              this.setState({ currCase: null, visible: false });
            }}
            type={this.props.type}
          />
        )}
      </div>
    );
  }
}
export default Form.create()(CaseLists);

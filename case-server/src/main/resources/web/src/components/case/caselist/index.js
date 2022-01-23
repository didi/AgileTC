/* eslint-disable */
import React from 'react';
import PropTypes from 'prop-types';
import request from '@/utils/axios';
import { Row, Button, Col, Icon, Form, message } from 'antd';
import './index.scss';
import _ from 'lodash';
import CaseModal from './caseModal.js';
import List from './list.js';
import Filter from './filter.js';
import OeFilter from './oefilter';
import FileTree from './tree';
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
      filterStatus: 'filter-hide',
      filterVisble: false,
      loading: true,
      current: 1,
      productLineId: '',
      treeData: [],
      levelId: '',
      levelText: '',
      searchValue: '',
      autoExpandParent: true,
      dataList: [],
      caseIds: ['root'],
      isSelect: true,
      isSibling: true,
      isAdd: true,
      isReName: true,
      treeSelect: [],
      treeData: [],
    };
  }
  componentDidMount() {
    this.setState(
      {
        productLineId: this.props.match.params.productLineId,
      },
      () => {
        this.getProductMumber();
        // this.getCaseList(1, '', '', '', []);
        this.getTreeList();
      },
    );
  }
  componentWillReceiveProps(nextProps) {
    if (
      this.props.match.params.productLineId !=
      nextProps.match.params.productLineId
    ) {
      this.setState(
        {
          productLineId: nextProps.match.params.productLineId,
        },
        () => {
          this.getCaseList(1, '', '', '', []);
          this.getProductMumber();
        },
      );
    }
  }
  getTreeList = isManual => {
    const { productLineId, caseIds } = this.state;
    const { doneApiPrefix } = this.props;
    return request(`${doneApiPrefix}/dir/list`, {
      method: 'GET',
      params: {
        productLineId,
        channel: 1,
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState(
          {
            treeData: res.data.children,
            caseIds:
              this.state.treeSelect.length > 0
                ? this.state.treeSelect.toString()
                : caseIds,
          },
          () => {
            if (!isManual) this.getCaseList(1, '', '', '', []);
          },
        );
      } else {
        message.error(res.msg);
      }
      return null;
    });
  };
  getCaseList = (
    current,
    nameFilter,
    createrFilter,
    iterationFilter,
    choiseDate = [],
    caseKeyWords,
  ) => {
    const { caseIds } = this.state;
    request(`${this.props.doneApiPrefix}/case/list`, {
      method: 'GET',
      params: {
        pageSize: 10,
        pageNum: current,
        productLineId: this.state.productLineId,
        caseType: 0,
        title: nameFilter || '',
        creator: createrFilter || '',
        channel: 1,
        requirementId: iterationFilter || '',
        beginTime: choiseDate.length > 0 ? `${choiseDate[0]} 00:00:00` : '',
        endTime: choiseDate.length > 0 ? `${choiseDate[1]}  23:59:59` : '',
        bizId: caseIds ? caseIds : 'root',
        caseKeyWords: caseKeyWords || '',
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState({
          list: res.data.dataSources,
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

  initCaseModalInfo = () => {
    let { requirementLs } = this.state;
    let requirement = null;
    this.setState({
      options: {
        requirement,
        requirementLs,
      },
    });
  };
  getProductMumber = () => {
    let url = `${this.props.doneApiPrefix}/case/listCreators`;
    request(url, {
      method: 'GET',
      params: { productLineId: this.state.productLineId, caseType: 0 },
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
      treeData,
      caseIds,
    } = this.state;
    const { match, doneApiPrefix } = this.props;
    const { productLineId } = match.params;
    return (
      <div className="all-content">
        <FileTree
          productLineId={Number(productLineId)}
          doneApiPrefix={doneApiPrefix}
          getCaseList={caseIds => {
            this.setState({ caseIds }, () => {
              this.getCaseList(1, '', '', '');
            });
          }}
          getTreeList={this.getTreeList}
          treeData={treeData}
        />
        <div className="min-hig-content">
          <div className="site-drawer-render-in-current-wrapper">
            <Row className="m-b-10">
              <Col span={18}>
                <div style={{ margin: '10px' }}>
                  快速筛选：<a>全部({total})</a>
                </div>
              </Col>
              <Col xs={6} className="text-right">
                <Button
                  style={{ marginRight: 16 }}
                  onClick={this.filterHandler}
                >
                  <Icon type="filter" /> 筛选
                </Button>
                <Button
                  type="primary"
                  onClick={() => {
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
              productId={productLineId}
              options={this.state.options}
              list={list}
              total={total}
              handleTask={this.handleTask}
              getCaseList={this.getCaseList}
              getTreeList={this.getTreeList}
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

            {(filterVisble && (
              <OeFilter
                onCancel={this.closeFilter}
                getCaseList={this.getCaseList}
                productMember={productMember}
                filterStatus={filterStatus}
                closeFilter={this.closeFilter}
                visible={filterVisble}
                oeApiPrefix={this.props.oeApiPrefix}
                productId={productLineId}
              />
            )) ||
              null}
          </div>

          {this.state.visible && (
            <CaseModal
              productId={productLineId}
              data={this.state.currCase}
              title={this.state.title}
              requirement={requirement}
              options={this.state.options}
              show={this.state.visible}
              onClose={this.onClose}
              oeApiPrefix={this.props.oeApiPrefix}
              doneApiPrefix={this.props.doneApiPrefix}
              baseUrl={this.props.baseUrl}
              onUpdate={() => {
                // this.getCaseList(this.state.current || 1, '', '', '', []);
                this.getTreeList();
                this.setState({ currCase: null, visible: false });
              }}
              type={this.props.type}
              caseIds={caseIds}
            />
          )}
        </div>
      </div>
    );
  }
}
export default Form.create()(CaseLists);

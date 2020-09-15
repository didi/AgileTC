/* eslint-disable */
import React, { Component } from 'react';
import request from '@/utils/axios';
import router from 'umi/router';
import {
  Table,
  Icon,
  Button,
  Card,
  Tooltip,
  Modal,
  message,
  Pagination,
  Checkbox,
} from 'antd';
import './index.scss';
import moment from 'moment';
moment.locale('zh-cn');
import TestModal from './testModal';
import CaseModal from './caseModal';
import getQueryString from '@/utils/getCookies';
import debounce from 'lodash/debounce';
const getCookies = getQueryString.getCookie;
const initData = `{"root":{"data":{"id":"bv8nxhi3c800","created":1562059643204,"text":"中心主题"},"children":[]},"template":"default","theme":"fresh-blue","version":"1.4.43"}`;

class TestTask extends Component {
  constructor(props) {
    super(props);
    this.state = {
      list: [],
      total: 0,
      loading: false,
      pageNum: 1,
      titleModeTask: '新建测试任务',
      visible: false,
      record: {},
      choiseDate: [],
      radioValue: '0',
      selectValue: [],
      switchValue: true,
      addCaseVisible: false,
      caseList: [], // 关联用例列表
      ownerList: [],
      fetching: false,
      requirementSeach: '',
      caseInfo: {}, // 根据关联用例查询case信息
      caseId: '', // 新建任务——关联用例
      options: [],
      checked: false,
      resource: [], // 自定义标签列表
    };
    this.lastFetchId = 0;
    this.getOwnerList = debounce(this.getOwnerList, 800);
  }
  componentDidMount = () => {
    this.getTestList();
    this.getRequirementList();
  };
  componentWillReceiveProps(nextProps) {
    this.setState(nextProps);
    if (!nextProps.visible) {
      this.setState({ requirementSeach: '' });
    }
    if (this.props.num !== nextProps.num && this.props.num) {
      this.setState(
        {
          pageNum: 1,
        },
        () => {
          this.getTestList();
        },
      );
    }
  }
  clearRequire = () => {
    this.setState({ requirementSeach: '' });
  };
  setColumns = () => {
    const columns = [
      {
        title: '任务ID',
        dataIndex: 'id',
        key: 'id',
      },
      {
        title: '任务名称',
        dataIndex: 'title',
        key: 'title',
        render: (t, record) => {
          let url = `${this.props.baseUrl}/caseManager/${this.props.productId}/${record.caseId}/${record.id}/3`;
          return <a onClick={() => this.taskLink(url, record)}>{t}</a>;
        },
      },
      {
        title: () => (
          <Tooltip placement="top" title="参与标记用例结果的人员列表">
            <span style={{ cursor: 'pointer' }}>执行人</span>
          </Tooltip>
        ),
        dataIndex: 'executors',
        key: 'executors',
      },
      {
        title: () => (
          <Tooltip placement="top" title="负责执行任务与标记用例结果">
            <span style={{ cursor: 'pointer' }}>负责人</span>
          </Tooltip>
        ),
        dataIndex: 'owner',
        key: 'owner',
        render: text => (
          <Tooltip title={text}>
            <span className="table-ellipsis">{text}</span>
          </Tooltip>
        ),
      },
      {
        title: '通过率',
        dataIndex: 'passRate',
        key: 'passRate',
        render: (t, record) => {
          return <div>{t}%</div>;
        },
      },
      {
        title: '已测用例集',
        dataIndex: 'passCount',
        key: 'passCount',
        render: (t, record) => {
          return (
            <div>
              {t}
              <span style={{ color: 'rgb(145, 164, 193)' }}>
                /{record.totalCount}
              </span>
            </div>
          );
        },
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreated',
        render: text => {
          return <div>{moment(text).format('YYYY-MM-DD HH:mm:ss')}</div>;
        },
      },
      {
        title: '操作',
        dataIndex: 'handle',
        key: 'handle',
        render: (text, record) => {
          let creator = getCookies('username');
          let url = `/case/caseManager/${this.props.productId}/${record.caseId}/${record.id}/3`;
          if (this.props.type == 'oe') {
            url = `${this.props.baseUrl}/caseManager/${this.props.productId}/${record.caseId}/${record.id}/3`;
          }
          return (
            <span>
              <Tooltip title="编辑测试任务">
                <a
                  onClick={() => this.showTestModal('编辑测试任务', record)}
                  className="edit"
                >
                  <Icon type="edit" />
                </a>
              </Tooltip>
              <Tooltip title="执行测试">
                <a
                  className="implement"
                  onClick={() => this.taskLink(url, record)}
                >
                  <Icon type="file-done" />
                </a>
              </Tooltip>
              {record.creator !== creator ? (
                <Tooltip title={`只允许创建者：${record.creator}删除`}>
                  <a className="delete iconcolor">
                    <Icon type="delete" />
                  </a>
                </Tooltip>
              ) : (
                <a
                  onClick={() => {
                    Modal.confirm({
                      title: '确认删除测试任务吗',
                      content: (
                        <span>
                          这将删除该测试任务下所有的测试与测试结果等信息，并且不可撤销。{' '}
                          <br />
                          <Checkbox
                            onChange={this.onChangeCheckbox}
                            style={{ marginTop: 20 }}
                          >
                            我明白以上操作
                          </Checkbox>
                        </span>
                      ),
                      onOk: e => {
                        if (this.state.checked) {
                          this.delete(record);
                          Modal.destroyAll();
                        } else {
                          message.info('请先勾选我已明白以上操作');
                        }
                      },
                      icon: <Icon type="exclamation-circle" />,
                      cancelText: '取消',
                      okText: '删除',
                    });
                  }}
                  className="delete"
                >
                  <Icon type="delete" />
                </a>
              )}
            </span>
          );
        },
      },
    ];
    return columns;
  };
  // 任务名称跳转
  taskLink = (url, record) => {
    let loginUser = getCookies('username');
    if (record.owner === '' || record.owner.indexOf(loginUser) > -1) {
      router.push(url);
    } else {
      this.showConfirm(url);
    }
  };
  // 任务名称跳转、执行测试confirm弹框
  showConfirm = url => {
    return Modal.confirm({
      title: '您不是当前测试任务指派的负责人，确认要执行该任务？',
      onOk() {
        router.push(url);
      },
      onCancel() {},
      icon: <Icon type="question-circle" style={{ color: '#1890FF' }} />,
      cancelText: '取消',
      okText: '确认',
    });
  };
  // 获取测试任务列表
  getTestList = () => {
    this.setState({ loading: true });
    request(
      `/${this.props.doneApiPrefix}/execRecord/getRecordByRequirementId`,
      {
        method: 'GET',
        params: {
          requirementId: this.props.match.params.requirementId,
          pageNum: this.state.pageNum,
          pageSize: 20,
          channel: this.props.type === 'oe' ? 1 : 0,
        },
      },
    ).then(res => {
      if (res.code === 200) {
        this.setState({
          list: res.data.data === null ? [] : res.data.data,
          total: res.data.total,
        });
      }
      this.setState({ loading: false });
    });
  };
  // 获取需求列表
  getRequirementList = () => {
    // request(`/${this.props.oeApiPrefix}/business-lines/requirements`, {
    //   method: 'GET',
    //   params: {
    //     requirementIds: this.props.match.params.requirementId,
    //   },
    // }).then(res => {
    //   this.setState({ options: res });
    // });
  };
  // 分页
  onChangePagination = page => {
    this.setState({ pageNum: page }, () => {
      this.getTestList();
    });
  };
  // 获取关联用例列表
  getCaseList = () => {
    request(`/${this.props.doneApiPrefix}/case/list`, {
      method: 'GET',
      params: {
        pageSize: 9999,
        pageNum: 1,
        case_type: 0,
        channel: this.props.type === 'oe' ? 1 : 0,
        productLineId: this.props.match.params.productId,
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState({ caseList: res.data ? res.data.data : [] });
      }
    });
  };
  getOwnerList = value => {
    if (!value) {
      return;
    }
    this.lastFetchId += 1;
    const fetchId = this.lastFetchId;
    this.setState({ requirementSeach: value, fetching: true });
    request(`/${this.props.oeApiPrefix}/user/suggest`, {
      method: 'GET',
      params: {
        username: value,
        onlyEmployee: false,
      },
    }).then(res => {
      if (fetchId !== this.lastFetchId) {
        return;
      }
      //  if (res.code === 200) {
      this.setState({ ownerList: res ? res : [], fetching: false });
      //  }
    });
  };
  // 获取测试任务列表
  getCaseNum = () => {
    if (this.state.caseId) {
      let url = '/case/countByCondition';
      if (this.props.type === 'oe') {
        url = `/${this.props.doneApiPrefix}/case/countByCondition`;
      }
      request(url, {
        method: 'POST',
        body: {
          caseId: Number(this.state.caseId),
          priority: this.state.selectValue,
          resource: this.state.resource || [],
        },
      }).then(res => {
        if (res.code === 200) {
          this.setState({ caseInfo: res.data });
        } else {
          this.setState({ caseInfo: {} });
        }
      });
    }
  };
  onChangeCheckbox = e => {
    this.setState({ checked: e.target.checked });
  };
  // 新建编辑测试任务弹框
  showTestModal = (title, record) => {
    this.getCaseList();
    this.setState({
      titleModeTask: title,
      visible: true,
      record: record ? record : {},
    });
    if (title === '编辑测试任务') {
      const choiseDate = [];
      if (record.expectStartTime && record.expectEndTime) {
        choiseDate.push(
          moment(record.expectStartTime).format('YYYY-MM-DD HH:mm:ss'),
        );
        choiseDate.push(
          moment(record.expectEndTime).format('YYYY-MM-DD HH:mm:ss'),
        );
      }
      this.setState(
        {
          radioValue: JSON.parse(record.chooseContent).priority.some(
            it => it === '0',
          )
            ? '0'
            : '1',
          selectValue: JSON.parse(record.chooseContent).priority.some(
            it => it === '0',
          )
            ? []
            : JSON.parse(record.chooseContent).priority,
          resource: JSON.parse(record.chooseContent).resource || [],
          switchValue: record.create,
          choiseDate,
          caseId: record.caseId,
        },
        () => {
          this.getCaseNum();
        },
      );
    }
  };
  // 日期选择
  onDataChange = (value, dateString) => {
    this.setState({ choiseDate: dateString });
  };
  // 选择用例
  radioOnChange = e => {
    this.setState(
      {
        radioValue: e.target.value,
        selectValue: [],
        resource: [],
      },
      () => {
        this.getCaseNum();
      },
    );
  };
  // 手动圈选用例
  handleChangeSelect = value => {
    this.setState({ selectValue: value }, () => {
      this.getCaseNum();
    });
  };
  // 自定义标签
  handleChangeTagSelect = value => {
    this.setState({ resource: value }, () => {
      this.getCaseNum();
    });
  };
  // 获取用例相关数据
  caseChange = (e, form) => {
    this.setState(
      {
        caseId: e,
        radioValue: '0',
        selectValue: [],
        resource: [],
      },
      () => {
        this.getCaseNum();
        form.setFieldsValue({ chooseContent: '0' });
      },
    );
  };
  // 弹窗是否新建
  switchChange = e => {
    this.setState({ switchValue: e });
  };
  // 新建用例弹框
  addCase = () => {
    this.setState({ addCaseVisible: true });
  };
  // 关闭用例弹框
  onCaseClose = form => {
    form.resetFields();
    this.setState({
      addCaseVisible: false,
    });
  };
  // 确认新增用例
  handleCaseOk = (form, xmindFile) => {
    form.validateFields((err, values) => {
      if (!err) {
        console.log('===', err);
        let requirementId = this.props.match.params.requirementId;
        // if(this.props.type === 'oe'){
        //   requirementId = values.requirementId.map(item => item.key.split('-')[0]).join(',')
        // }
        let params = {
          groupId: '1',
          channel: '0',
          title: values.case,
          description: values.description,
          creator: getCookies('username'),
          isDelete: 0,
          productLineId: this.props.match.params.productId,
          caseContent: initData,
          requirementId: requirementId,
          caseType: 0,
          channel: this.props.type === 'oe' ? 1 : 0,
        };
        // 判断是否上传了xmind文件
        let { type } = this.props;
        let url =
          type === 'oe'
            ? `/${this.props.doneApiPrefix}/case/create`
            : '/case/create';
        if (xmindFile) {
          url =
            type === 'oe'
              ? `/${this.props.doneApiPrefix}/file/import`
              : '/file/import';
          params = new FormData();
          params.append('file', xmindFile);
          params.append('creator', getCookies('username'));
          params.append('caseTitle', values.case);
          params.append('productLine', this.props.match.params.productId);
          params.append('requirementId', requirementId);
          params.append('description', values.description);
          // params.append('owner', values.owner ? values.owner.join(',') : 'import')
          params.append('caseType', 0);
          params.append('channel', this.props.type === 'oe' ? 1 : 0);
        }
        request(url, {
          method: 'POST',
          body: params,
        }).then(res => {
          if (res.code == 200) {
            message.success('新建测试用例集成功');
            this.onCaseClose(form);
            this.getCaseList();
          } else {
            message.error(res.msg);
          }
        });
      }
    });
  };
  // 关闭测试任务弹框
  onClose = (form, action) => {
    form.resetFields();
    this.setState({
      visible: action === 'go' ? true : false,
      titleModeTask: '新建测试任务',
      ownerList: [],
      record: {},
      radioValue: '0',
      choiseDate: [],
      selectValue: [],
      resource: [],
      switchValue: true,
      addCaseVisible: false,
      caseInfo: {},
      caseId: '',
    });
  };
  // 确认新增测试
  handleOk = (form, action) => {
    form.validateFields((err, values) => {
      if (!err) {
        values.chooseContent = JSON.stringify({
          priority:
            values.chooseContent === '0' ? ['0'] : this.state.selectValue,
          resource: this.state.resource,
        });
        let params = values;
        const { choiseDate } = this.state;
        let url = '';
        if (this.state.titleModeTask !== '编辑测试任务') {
          if (values.create) {
            url =
              this.props.type !== 'oe'
                ? `/execRecord/addRecord`
                : `/${this.props.doneApiPrefix}/execRecord/addRecord`;
            params.caseId = values.relation;
            params.creator = getCookies('username');
            params.owner = values.owner.join(',');
            params.expectStartTime = choiseDate[0]
              ? moment(choiseDate[0])
                  .startOf('day')
                  .valueOf()
              : '';
            params.expectEndTime = choiseDate[0]
              ? moment(choiseDate[1])
                  .endOf('day')
                  .valueOf()
              : '';
            delete params.cyclePlan;
            this.addEdit(url, params, form, action, true);
          }
          url =
            this.props.type !== 'oe'
              ? `/case/caseAddRequirementId`
              : `/${this.props.doneApiPrefix}/case/caseAddRequirementId`;
          params.id = values.relation;
          params.requirementId = this.props.match.params.requirementId;
          delete params.cyclePlan;
          delete params.relation;
          delete params.creator;
          delete params.expectStartTime;
          delete params.expectEndTime;
          delete params.caseId;
          this.addEdit(url, params, form, action);
        } else {
          url =
            this.props.type !== 'oe'
              ? `/execRecord/EditRecord`
              : `/${this.props.doneApiPrefix}/execRecord/EditRecord`;
          delete params.cyclePlan;
          delete params.creator;
          params.owner = values.owner.join(',');
          params.id = this.state.record.id;
          params.expectStartTime = choiseDate[0]
            ? moment(choiseDate[0])
                .startOf('day')
                .valueOf()
            : '';
          params.expectEndTime = choiseDate[0]
            ? moment(choiseDate[1])
                .endOf('day')
                .valueOf()
            : '';
          params.modifier = getCookies('username');
          this.addEdit(url, params, form, action);
        }
      }
    });
  };
  addEdit = (url, params, form, action, msg) => {
    request(url, {
      method: 'POST',
      body: params,
    }).then(res => {
      if (res.code == 200) {
        if (this.state.titleModeTask === '编辑测试任务') this.getTestList();
        if (!msg)
          message.success(
            this.state.titleModeTask === '编辑测试任务'
              ? '更新成功'
              : '创建成功',
          );
        if (this.state.titleModeTask !== '编辑测试任务') this.props.addNum();
        this.onClose(form, action);
      } else {
        message.error(res.msg);
      }
    });
  };
  // 保存并继续
  saveGo = form => {
    form.validateFields((err, values) => {
      if (!err) {
        this.handleOk(form, 'go');
        this.getTestList();
      }
    });
  };
  // 删除测试任务
  delete = record => {
    let url = `/execRecord/softdelete`;
    if (this.props.type === 'oe') {
      url = `/${this.props.doneApiPrefix}/execRecord/softdelete`;
    }
    request(url, {
      method: 'POST',
      body: {
        id: record.id,
      },
    }).then(res => {
      if (res.code == 200) {
        message.success('删除成功');
        this.getTestList();
        this.setState({ checked: false });
      } else {
        message.error(res.msg);
      }
    });
  };

  render() {
    const {
      list,
      total,
      loading,
      pageNum,
      titleModeTask,
      visible,
      record,
      choiseDate,
      radioValue,
      selectValue,
      switchValue,
      addCaseVisible,
      caseList,
      caseInfo,
      caseId,
      options,
      ownerList,
      fetching,
      requirementSeach,
      resource,
    } = this.state;
    return (
      <React.Fragment>
        {this.props.useType === 'button' ? (
          <Button
            icon="plus"
            onClick={() => this.showTestModal('新建测试任务')}
          >
            测试任务
          </Button>
        ) : (
          <Card
            style={{ width: '100%' }}
            bordered={false}
            className="task_card"
          >
            <div className="card_title">
              <span>测试任务</span>
            </div>
            <Table
              columns={this.setColumns()}
              dataSource={list}
              className={total > 20 ? 'table-test' : 'table-test-footer'}
              rowKey="id"
              loading={loading}
              pagination={false}
              size="small"
              locale={{
                emptyText: (
                  <div>
                    <Icon type="frown-o" /> 暂无数据
                  </div>
                ),
              }}
              footer={currentData =>
                total > 20 ? (
                  <div className="pagination">
                    <Pagination
                      onChange={this.onChangePagination}
                      current={pageNum}
                      total={Number(total)}
                      pageSize={20}
                      size="small"
                    />
                  </div>
                ) : null
              }
            />
          </Card>
        )}
        {visible && (
          <TestModal
            visible={visible}
            titleModeTask={titleModeTask}
            record={record}
            choiseDate={choiseDate}
            radioValue={radioValue}
            selectValue={selectValue}
            switchValue={switchValue}
            caseList={caseList}
            ownerList={ownerList}
            fetching={fetching}
            requirementSeach={requirementSeach}
            caseInfo={caseInfo}
            caseId={caseId}
            resource={resource}
            getOwnerList={this.getOwnerList}
            clearRequire={this.clearRequire}
            onClose={this.onClose}
            handleOk={this.handleOk}
            onDataChange={this.onDataChange}
            radioOnChange={this.radioOnChange}
            handleChangeSelect={this.handleChangeSelect}
            handleChangeTagSelect={this.handleChangeTagSelect}
            switchChange={this.switchChange}
            addCase={this.addCase}
            saveGo={this.saveGo}
            caseChange={this.caseChange}
          />
        )}
        {addCaseVisible && (
          <CaseModal
            visible={addCaseVisible}
            options={options}
            requirementId={this.props.match.params.requirementId}
            onClose={this.onCaseClose}
            handleOk={this.handleCaseOk}
          />
        )}
      </React.Fragment>
    );
  }
}
export default TestTask;

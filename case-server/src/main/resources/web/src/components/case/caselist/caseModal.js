/* eslint-disable */
import React from 'react';
import PropTypes from 'prop-types';
import router from 'umi/router';
import {
  Upload,
  Form,
  Select,
  message,
  Modal,
  Input,
  Icon,
  Row,
  Col,
} from 'antd';
const { Dragger } = Upload;
import './index.scss';
const initData = `{"root":{"data":{"id":"bv8nxhi3c800","created":1562059643204,"text":"中心主题"},"children":[]},"template":"default","theme":"fresh-blue","version":"1.4.43"}`;
const Option = Select.Option;
const formItemLayout = {
  labelCol: { span: 6 },
  wrapperCol: { span: 16 },
};
import request from '@/utils/axios';
import getQueryString from '@/utils/getCookies';
const getCookies = getQueryString.getCookie;
const { TextArea } = Input;
import debounce from 'lodash/debounce';
/* global moment, cardStatusList, priorityList, staffNameCN, staffNamePY */
class CaseModal extends React.Component {
  static propTypes = {
    show: PropTypes.bool,
    productId: PropTypes.any,
    requirementId: PropTypes.any,
    product: PropTypes.object,
    project: PropTypes.object,
    requirement: PropTypes.object,
    data: PropTypes.object,
    options: PropTypes.object,
    form: PropTypes.object,
    onUpdate: PropTypes.func,
    onClose: PropTypes.func,
  };
  constructor(props) {
    super(props);
    let { product, project, requirement, options, data, title } = this.props;
    this.state = {
      title: '',
      show: this.props.show,
      iterationList: [], // 需求列表
      nameFilter: '', // 用例名称筛选最终选择
      xmindFile: null, // 保存上传的file文件，单文件
      productId: this.props.productId,
      requirementId: this.props.requirementId,
      operate: title,
      data: data,
      product: product,
      project: project,
      requirement: requirement,
      options: options,
      value: [],
      fetching: false,
      requirementOe: [],
      currProjectId: project ? project.id : null,
      requirementArr: [],
      requirementSeach: '',
    };
    this.lastFetchId = 0;
    this.getOeRequirement = debounce(this.getOeRequirement, 800);
  }
  componentDidMount() {
    this.props.data &&
      this.props.data.requirementId &&
      this.getRequirementsById(this.props.data.requirementId);
  }
  componentWillReceiveProps(nextProps) {
    this.setState(nextProps);
    if (!nextProps.show) {
      this.props.form.resetFields();
      this.setState({ requirementSeach: '' });
    }
    if (nextProps.show && nextProps.show !== this.state.show) {
      let { project, options, product, requirement } = nextProps;

      this.setState({
        data: nextProps.data,
        requirementId: requirement ? requirement.id : null,
        product: product,
        requirement: requirement,
        project: project,
        options: options,
        currProjectId: project ? project.id : null,
      });
    }
  }
  getRequirementsById = requirementIds => {
    // request(`${this.props.oeApiPrefix}/business-lines/requirements`, {
    //   method: 'GET',
    //   params: { requirementIds: requirementIds },
    // }).then(res => {
    //   this.setState({ requirementArr: res });
    // });
  };
  handleOk = () => {
    const { operate } = this.state;
    if (operate != 'edit') {
      this.props.form.validateFields((err, values) => {
        if (!err) {
          this.saveEditerData(values);
        }
      });
    } else {
      this.props.form.validateFields((err, values) => {
        if (!err) {
          this.renameOk(values);
        }
      });
    }
  };
  // 确认新建
  saveEditerData(values) {
    let requirementId = values.requirementId;
    // if (this.props.type == 'oe') {
    //   requirementId = values.requirementId
    //     .map(item => item.key.split('-')[0])
    //     .join(',');
    // }
    let params = {
      groupId: '1',
      channel: '0',
      title: values.case,
      description: values.description,
      creator: getCookies('username'),
      isDelete: 0,
      productLineId: this.props.productId,
      caseContent: initData,
      requirementId,
      caseType: 0,
      id: this.state.operate != 'add' ? this.props.data.id : '',
      channel: this.props.type === 'oe' ? 1 : 0,
    };

    // 判断是否上传了xmind文件
    let xmindFile = this.state.xmindFile;
    let { type } = this.props;

    let url =
      type === 'oe'
        ? `${this.props.doneApiPrefix}/case/create`
        : '/case/create';
    if (xmindFile) {
      url =
        type === 'oe'
          ? `${this.props.doneApiPrefix}/file/import`
          : '/file/import';
      params = new FormData();
      params.append('file', xmindFile);
      params.append('creator', getCookies('username'));
      params.append('caseTitle', values.case);
      params.append('productLine', this.props.productId);
      params.append('requirementId', requirementId);

      params.append('description', values.description);
      params.append('caseType', 0);
      params.append('channel', this.props.type === 'oe' ? 1 : 0);
    }
    request(url, { method: 'POST', body: params }).then(res => {
      if (res.code == 200) {
        message.success(
          this.state.operate == 'add'
            ? '新建测试用例集成功'
            : '复制测试用例集成功',
        );
        if (this.state.operate === 'add') {
          let urls = `${this.props.baseUrl}/caseManager/${this.props.productId}/${res.data}/undefined/0`;
          router.push(urls);
        }

        this.props.onClose(false);
        this.props.onUpdate && this.props.onUpdate();
      } else {
        message.error(res.msg);
      }
    });
  }
  // 确认重命名
  renameOk = values => {
    let requirementId = values.requirementId;
    // if (this.props.type == 'oe') {
    //   requirementId = values.requirementId
    //     .map(item => item.key.split('-')[0])
    //     .join(',');
    // }

    let params = {
      title: values.case,
      id: this.state.data.id,
      requirementId,
      caseType: 0,
      description: values.description,
      modifier: getCookies('username'),
    };

    let { type } = this.props;

    let url =
      type === 'oe'
        ? `${this.props.doneApiPrefix}/case/update`
        : '/case/update';
    request(url, { method: 'POST', body: params }).then(res => {
      if (res.code == 200) {
        this.props.onUpdate && this.props.onUpdate();
        message.success('更新成功');
      } else {
        message.error(res.msg);
      }
    });
  };

  getOeRequirement = title => {
    this.lastFetchId += 1;
    const fetchId = this.lastFetchId;
    this.setState({ requirementSeach: title, fetching: true });
    request(
      `${this.props.oeApiPrefix}/business-lines/${this.props.productId}/requirements`,
      { method: 'GET', params: { title: title, pageNum: 1, pageSize: 25 } },
    ).then(res => {
      let { requirementDetails } = res;
      if (fetchId !== this.lastFetchId) {
        return;
      }
      this.setState({ requirementOe: requirementDetails, fetching: false });
    });
  };

  render() {
    const {
      xmindFile,
      data,
      show,
      // currProjectId,
      project,
      requirement,
      options,
      operate,
      // requirementArr,
      // fetching,
      // requirementSeach,
    } = this.state;
    const { type } = this.props;
    const isOE = type === 'oe';
    const { getFieldDecorator } = this.props.form;
    const props = {
      accept: '.xmind',
      onRemove: file => {
        this.setState(state => ({ xmindFile: null }));
      },
      beforeUpload: file => {
        this.setState(state => ({ xmindFile: file }));
        const isLt2M = file.size / 1024 / 1024 <= 100;
        if (!isLt2M) {
          message.error('用例集文件大小不能超过100M');
        }
        return false;
      },
      fileList: xmindFile ? [xmindFile] : [],
    };
    let title = '';
    switch (operate) {
      case 'edit':
        title = '修改测试用例集';
        break;
      case 'add':
        title = '新增测试用例集';
        break;
      case 'copy':
        title = `复制测试用例集`;
        break;
      default:
        break;
    }

    // let newRequirementArr =
    //   requirementArr &&
    //   requirementArr.map(item => {
    //     // return {
    //     //   label: item.title,
    //     //   key: `${item.requirementId}-${item.title}`,
    //     // };
    //     return item.requirementId;
    //   });
    return (
      <Modal
        visible={show}
        onCancel={() => this.props.onClose && this.props.onClose(false)}
        onOk={this.handleOk}
        maskClosable={false}
        wrapClassName="oe-caseModal-style-wrap"
        title={title}
        okText="确认"
        cancelText="取消"
        width="600px"
      >
        <Form.Item {...formItemLayout} label="用例集名称：">
          {getFieldDecorator('case', {
            rules: [{ required: true, message: '请填写用例集名称' }],
            initialValue: data
              ? (operate == 'copy' && `copy of ${data.title}`) || data.title
              : '',
          })(<Input placeholder="请填写用例集名称" />)}
        </Form.Item>
        {(!isOE && (
          <Form.Item {...formItemLayout} label="所属项目">
            {getFieldDecorator('iteration', {
              rules: [{ required: true, message: '请选择所属项目' }],
              initialValue: project && operate != 'copy' ? project.id : '',
            })(
              <Select
                style={{ width: '100%' }}
                placeholder="所属项目"
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) => {
                  return (
                    option.props.children
                      .toLowerCase()
                      .indexOf(input.toLowerCase()) >= 0
                  );
                }}
                onChange={value => {
                  let project = _.find(options.projectLs, function(item) {
                    return item.id == value;
                  });
                  this.props.form.setFieldsValue({ requirementId: '' });
                  this.setState({
                    currProjectId: value,
                    project: project,
                    requirement: null,
                  });
                }}
              >
                {options &&
                  options.projectLs &&
                  options.projectLs.map((item, index) => {
                    return (
                      <Option key={item.id} value={item.id}>
                        {item.name}
                      </Option>
                    );
                  })}
              </Select>,
            )}
          </Form.Item>
        )) ||
          null}

        {(!isOE && (
          <Form.Item {...formItemLayout} label="所属需求：">
            {getFieldDecorator('requirementId', {
              rules: [{ required: true, message: '请选择所属需求' }],
              initialValue:
                requirement && requirement.status != '关闭' && operate != 'copy'
                  ? requirement.id
                  : '',
            })(<Input style={{ Width: '100%' }} placeholder="所属需求" />)}
          </Form.Item>
        )) || (
          <Form.Item {...formItemLayout} label="关联需求：">
            {getFieldDecorator('requirementId', {
              initialValue: data ? data.requirementId : undefined,
              // (this.state.operate !== 'copy' &&
              //   newRequirementArr.join(',')) ||
              // '',
            })(<Input placeholder="关联需求" style={{ width: '100%' }} />)}
          </Form.Item>
        )}
        {(isOE && (
          <Form.Item {...formItemLayout} label="描述：">
            {getFieldDecorator('description', {
              initialValue: data ? data.description : '',
            })(<TextArea autoSize={{ minRows: 4 }} maxLength="1024" />)}
          </Form.Item>
        )) ||
          null}

        {operate == 'add' && (
          <Row style={{ marginBottom: '20px' }}>
            <Col span={6}>导入本地xmind:</Col>
            <Col span={16} className="dragger">
              <div className="div-flex-child-1">
                <Dragger {...props}>
                  {xmindFile === null ? (
                    <Icon
                      type="plus-circle"
                      style={{ color: '#447CE6', fontSize: '24px' }}
                    />
                  ) : (
                    <Icon
                      type="file"
                      style={{
                        color: '#447CE6',
                        fontSize: '24px',
                        position: 'relative',
                        top: '-15px',
                      }}
                    />
                  )}
                </Dragger>
              </div>
              <div className="div-flex-child-2">
                <div>
                  <span className="span-text span-text-bold">
                    上传文件（非必传）
                  </span>
                  <span className="span-text span-text-light">
                    仅支持.xmind扩展名文件...
                  </span>
                </div>
              </div>
            </Col>
          </Row>
        )}
      </Modal>
    );
  }
}
export default Form.create()(CaseModal);

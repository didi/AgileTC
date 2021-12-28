/* eslint-disable */
import React from 'react';
import {
  Form,
  Select,
  message,
  Modal,
  Input,
  DatePicker,
  Radio,
  Row,
  Col,
} from 'antd';
import './index.scss';
import moment from 'moment';
const Option = Select.Option;
const formItemLayout = {
  labelCol: { span: 6 },
  wrapperCol: { span: 16 },
};
import request from '@/utils/axios';
import getQueryString from '@/utils/getCookies';
const getCookies = getQueryString.getCookie;
const { RangePicker } = DatePicker;

const { TextArea } = Input;
class TaskModal extends React.Component {
  static propTypes = {};
  constructor(props) {
    super(props);

    this.state = {
      radioValue: '0',
      selectValue: [],
      choiseDate: [],
      resource: [],
    };
  }

  componentWillReceiveProps(nextProps) {
    if (
      nextProps.titleModeTask == '编辑测试任务' &&
      nextProps.visible != this.props.visible
    ) {
      this.setState(
        {
          radioValue: this.handleChooseContent(nextProps.record.chooseContent)
            .content,
          selectValue: this.handleChooseContent(nextProps.record.chooseContent)
            .priority,
          resource: nextProps.record.chooseContent
            ? JSON.parse(nextProps.record.chooseContent).resource
            : [],
          choiseDate:
            nextProps.record.expectStartTime && nextProps.record.expectEndTime
              ? [
                  nextProps.record.expectStartTime,
                  nextProps.record.expectEndTime,
                ]
              : [],
        },
        () => {},
      );
    }

    if (
      nextProps.titleModeTask == '新建测试任务' &&
      nextProps.visible != this.props.visible
    ) {
      this.setState({
        radioValue: '0',
        selectValue: [],
        choiseDate: [],
        resource: [],
      });
    }
  }

  handleOk = () => {
    this.props.form.validateFields((err, values) => {
      if (!err) {
        values.chooseContent = JSON.stringify({
          priority:
            values.chooseContent === '0' ? ['0'] : this.state.selectValue,
          resource: this.state.resource,
        });

        this.saveTaskData(values);
      }
    });
  };
  saveTaskData = values => {
    let params = values;
    const { choiseDate } = this.state;
    params.caseId = this.props.record.id;
    params.creator = getCookies('username');
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
    // params.owner = params.owner.join(',');
    params.owner = params.owner || '';
    delete params.cyclePlan;
    let url = `${this.props.doneApiPrefix}/record/create`;

    if (this.props.titleModeTask == '编辑测试任务') {
      url = `${this.props.doneApiPrefix}/record/edit`;

      delete params.caseId;
      delete params.creator;

      params.id = this.props.record.id;
      params.modifier = getCookies('username');
    }
    request(url, { method: 'POST', body: params }).then(res => {
      if (res.code === 200) {
        this.props.handleOkTask(this.props.record);
        this.setState({ choiseDate: [], radioValue: '' });
        this.props.form.resetFields();
      } else {
        message.error(res.msg);
      }
    });
  };
  onDataChange = (value, dateString) => {
    this.setState({ choiseDate: dateString });
  };
  radioOnChange = value => {
    this.setState(
      {
        radioValue: value.target.value,
        selectValue: [],
        resource: [],
      },
      () => {
        this.props.getCaseInfo(this.state.selectValue, this.state.resource);
      },
    );
  };
  handleChangeSelect = value => {
    this.setState({ selectValue: value }, () => {
      this.props.getCaseInfo(value, this.state.resource);
    });
  };
  // 自定义标签
  handleChangeTagSelect = value => {
    this.setState({ resource: value }, () => {
      this.props.getCaseInfo(this.state.selectValue, value);
    });
  };

  handleChooseContent = content => {
    let val = content && JSON.parse(content).priority;

    let val1 = val.indexOf('0') > -1 ? '0' : '1';

    return {
      content: val1,
      priority: val1 === '1' ? val : [],
    };
  };

  render() {
    const { ownerList, requirementSeach, fetching } = this.props;
    const { getFieldDecorator } = this.props.form;
    const { choiseDate, radioValue, selectValue, resource } = this.state;
    const radioStyle = {
      display: 'block',
      height: '80px',
      marginTop: '10px',
      fontSize: '14px',
    };
    const grade = ['P0', 'P1', 'P2'];
    return (
      <Modal
        visible={this.props.visible}
        maskClosable={false}
        title={this.props.titleModeTask}
        okText="确认"
        cancelText="取消"
        closable
        wrapClassName="oe-taskModal-style-wrap"
        onCancel={() => {
          this.props.onClose(this.props.form);

          this.setState({ choiseDate: [] });
        }}
        onOk={this.handleOk}
        width="600px"
        className="task-modal"
      >
        <Form.Item {...formItemLayout} label="名称：">
          {getFieldDecorator('title', {
            rules: [{ required: true, message: '请输入名称' }],
            initialValue:
              this.props.titleModeTask == '编辑测试任务'
                ? this.props.record.title
                : '',
          })(<Input placeholder="请输入名称" />)}
        </Form.Item>

        <Form.Item {...formItemLayout} label="负责人">
          {getFieldDecorator('owner', {
            // initialValue:
            //   this.props.titleModeTask === '编辑测试任务' &&
            //   this.props.record.owner
            //     ? this.props.record.owner.split(',')
            //     : [],
            initialValue:
              this.props.titleModeTask === '编辑测试任务' &&
              this.props.record.owner
                ? this.props.record.owner
                : '',
          })(
            <Input placeholder="请输入负责人" />,
            // <Select
            //   mode="multiple"
            //   placeholder="请输入负责人"
            //   onSearch={value => {
            //     this.props.getOwnerList(value);
            //   }}
            //   filterOption={false}
            //   notFoundContent={
            //     fetching ? (
            //       <span>搜索中...</span>
            //     ) : requirementSeach ? (
            //       '搜索不到该负责人'
            //     ) : (
            //       '请输入负责人名进行搜索'
            //     )
            //   }
            //   onBlur={e => {
            //     this.props.clearRequire();
            //   }}
            // >
            //   {ownerList &&
            //     ownerList.map((item, index) => (
            //       <Option key={item.deptId} value={item.username}>
            //         {`${item.displayName}(${item.username})`}
            //       </Option>
            //     ))}
            // </Select>
          )}
        </Form.Item>

        <Form.Item {...formItemLayout} label="描述：">
          {getFieldDecorator('description', {
            initialValue:
              this.props.titleModeTask == '编辑测试任务'
                ? this.props.record.description
                : '',
          })(<TextArea />)}
        </Form.Item>
        <Form.Item {...formItemLayout} label="计划周期：">
          {getFieldDecorator('cyclePlan', {
            initialValue:
              choiseDate.length > 0
                ? [
                    moment(choiseDate[0], 'YYYY-MM-DD'),
                    moment(choiseDate[1], 'YYYY-MM-DD'),
                  ]
                : [],
          })(
            <RangePicker
              style={{ width: '100%' }}
              format={'YYYY-MM-DD'}
              placeholder={['开始时间', '结束时间']}
              onChange={this.onDataChange}
            />,
          )}
        </Form.Item>
        <Form.Item {...formItemLayout} label="选择用例集">
          {getFieldDecorator('chooseContent', {
            initialValue: radioValue,
          })(
            <Radio.Group onChange={this.radioOnChange}>
              <Radio style={radioStyle} value={'0'}>
                包含全部用例
                <br />
                <p className="small-size-font">
                  {' '}
                  覆盖全部可用用例（共计{' '}
                  {(this.props.record && this.props.caseInfo.totalCount) ||
                    '0'}{' '}
                  个），如果用例集库有新增的用例，会自动加入到本计划中
                </p>
              </Radio>
              <Radio style={radioStyle} value={'1'}>
                手动圈选用例集
                <br />
                <Row className="menu-case">
                  <Col span={4} style={{ textAlign: 'right' }}>
                    优先级：
                  </Col>
                  <Col span={20}>
                    <Select
                      style={{ width: 'calc(100% + 14px)', marginLeft: '6px' }}
                      disabled={radioValue !== '1'}
                      mode="multiple"
                      onChange={this.handleChangeSelect}
                      placeholder="用例等级"
                      value={selectValue}
                    >
                      {grade.map((item, index) => (
                        <Option key={index + 1}>{item}</Option>
                      ))}
                    </Select>
                  </Col>
                </Row>
                <Row className="menu-case">
                  <Col span={4} style={{ textAlign: 'right' }}>
                    标签：
                  </Col>
                  <Col span={20}>
                    <Select
                      disabled={radioValue !== '1'}
                      mode="multiple"
                      style={{ width: 'calc(100% + 14px)', marginLeft: '6px' }}
                      placeholder="请选择标签"
                      onChange={this.handleChangeTagSelect}
                      value={resource}
                    >
                      {this.props.caseInfo.taglist &&
                        this.props.caseInfo.taglist.length > 0 &&
                        this.props.caseInfo.taglist.map((item, index) => (
                          <Option key={item}>{item}</Option>
                        ))}
                    </Select>
                  </Col>
                </Row>
                {this.props.record &&
                  radioValue === '1' &&
                  ((selectValue && selectValue.length > 0) ||
                    (resource && resource.length > 0)) &&
                  (this.props.caseInfo.count ||
                    this.props.caseInfo.count === 0) && (
                    <span style={{ marginLeft: 22 }}>
                      {this.props.caseInfo.count}条用例集已选
                    </span>
                  )}
              </Radio>
            </Radio.Group>,
          )}
        </Form.Item>
      </Modal>
    );
  }
}
export default Form.create()(TaskModal);

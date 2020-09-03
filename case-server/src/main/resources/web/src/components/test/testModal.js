import React, { Component, Fragment } from 'react';
import { Modal, Form, Input, DatePicker, Radio, Select, Icon, Switch, Button, ConfigProvider, Row, Col } from 'antd';
import moment from 'moment';
import zhCN from 'antd/es/locale/zh_CN';
import 'moment/locale/zh-cn';
const { TextArea } = Input;
const { Option } = Select;
const { RangePicker } = DatePicker;
const formItemLayout = {
  labelCol: { span: 5 },
  wrapperCol: { span: 17 },
};

class TestModal extends Component {
  render() {
    const {
      visible,
      titleModeTask,
      record,
      choiseDate,
      radioValue,
      selectValue,
      switchValue,
      caseList,
      caseInfo,
      ownerList,
      requirementSeach,
      fetching,
      resource
    } = this.props;
    const { getFieldDecorator } = this.props.form;
    const grade = ['P0', 'P1', 'P2'];
    const radioStyle = {
      display: 'block',
      // height: '80px',
      marginTop: '10px',
      fontSize: '14px',
    };
    return (
      <Modal
        visible={visible}
        maskClosable={false}
        title={titleModeTask}
        wrapClassName="oe-testModal-test-wrap"
        okText="确认"
        cancelText="取消"
        closable
        onCancel={() => {
          this.props.onClose(this.props.form);
        }}
        width="700px"
        footer={
          <div>
            <Button
              onClick={() => {
                this.props.onClose(this.props.form);
              }}
            >
              取消
            </Button>
            {titleModeTask === '新建测试任务' && (
              <Button onClick={() => this.props.saveGo(this.props.form)} type="primary">
                保存继续添加
              </Button>
            )}
            <Button
              onClick={() => {
                this.props.handleOk(this.props.form);
              }}
              type="primary"
            >
              保存并关闭
            </Button>
          </div>
        }
      >
        <ConfigProvider locale={zhCN}>
          {titleModeTask === '新建测试任务' && (
            <Fragment>
              <Form.Item {...formItemLayout} label="关联用例集">
                {getFieldDecorator('relation', {
                  rules: [{ required: true, message: '请选择关联用例集' }],
                  initialValue: titleModeTask === '编辑测试任务' ? record.relation : [],
                })(
                  <Select
                    placeholder="请选择已有用例集"
                    style={{ width: '80%' }}
                    onChange={(e) => this.props.caseChange(e, this.props.form)}
                    allowClear
                    showSearch
                    optionFilterProp="children"
                    notFoundContent={<span>请选择用例集</span>}
                    filterOption={(input, option) => option.props.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
                  >
                    {caseList && caseList.length > 0 && caseList.map((item) => <Option key={item.id}>{item.title}</Option>)}
                  </Select>
                )}
                <a style={{ float: 'right' }} onClick={() => this.props.addCase()}>
                  <Icon type="plus" />
                  新建用例集
                </a>
              </Form.Item>
              <Form.Item {...formItemLayout} label="是否新建">
                {getFieldDecorator('create', {
                  initialValue: titleModeTask === '编辑测试任务' ? record.create : true,
                  valuePropName: 'checked',
                })(<Switch checkedChildren="是" unCheckedChildren="否" onChange={this.props.switchChange} />)}
              </Form.Item>
            </Fragment>
          )}
          {(switchValue || titleModeTask === '编辑测试任务') && (
            <Fragment>
              <Form.Item {...formItemLayout} label="名称：">
                {getFieldDecorator('title', {
                  rules: [{ required: true, message: '请输入名称' }],
                  initialValue: titleModeTask === '编辑测试任务' ? record.title : '',
                })(<Input placeholder="请输入名称" />)}
              </Form.Item>

              <Form.Item {...formItemLayout} label="负责人">
                {getFieldDecorator('owner', {
                  initialValue: titleModeTask === '编辑测试任务' && record.owner ? record.owner.split(',') : [],
                })(
                  <Select
                    mode="multiple"
                    placeholder="请输入负责人"
                    onSearch={(value) => {
                      this.props.getOwnerList(value);
                    }}
                    filterOption={false}
                    notFoundContent={fetching ? <span>搜索中...</span> : requirementSeach ? '搜索不到该负责人' : '请输入负责人名进行搜索'}
                    onBlur={(e) => {
                      this.props.clearRequire();
                    }}
                  >
                    {ownerList &&
                      ownerList.map((item, index) => (
                        <Option key={item.deptId} value={item.username}>
                          {`${item.displayName}(${item.username})`}
                        </Option>
                      ))}
                  </Select>
                )}
              </Form.Item>

              <Form.Item {...formItemLayout} label="描述：">
                {getFieldDecorator('description', {
                  initialValue: titleModeTask === '编辑测试任务' ? record.description : '',
                })(<TextArea placeholder="请填写..." />)}
              </Form.Item>
              <Form.Item {...formItemLayout} label="计划周期：">
                {getFieldDecorator('cyclePlan', {
                  initialValue: choiseDate.length > 0 ? [moment(choiseDate[0], 'YYYY-MM-DD'), moment(choiseDate[1], 'YYYY-MM-DD')] : null,
                })(
                  <RangePicker
                    format={'YYYY-MM-DD'}
                    placeholder={['开始时间', '结束时间']}
                    onChange={(value, dateString) => this.props.onDataChange(value, dateString)}
                    style={{ width: '100%' }}
                  />
                )}
              </Form.Item>
              {
                <Form.Item {...formItemLayout} label="选择用例集">
                  {getFieldDecorator('chooseContent', {
                    initialValue: radioValue,
                  })(
                    <Radio.Group onChange={(e) => this.props.radioOnChange(e)}>
                      <Radio style={radioStyle} value={'0'}>
                        包含全部用例
                        <p className="small-size-font">
                          {' '}
                          覆盖全部可用用例（共计 {caseInfo.totalCount || '0'} 个），如果用例集库有新增的用例，会自动加入到本计划中
                        </p>
                      </Radio>
                      <Radio style={radioStyle} value={'1'}>
                        手动圈选用例集
                        <br />
                        <Row className="menu-case">
                          <Col span={4} style={{ textAlign: 'right' }}>优先级：</Col>
                          <Col span={20}>
                            <Select
                              style={{ width: 'calc(100% + 14px)', marginLeft: '6px' }}
                              disabled={radioValue !== '1'}
                              mode="multiple"
                              onChange={(e) => this.props.handleChangeSelect(e)}
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
                          <Col span={4} style={{ textAlign: 'right' }}>标签：</Col>
                          <Col span={20}>
                            <Select
                              disabled={radioValue !== '1'}
                              mode="multiple"
                              style={{ width: 'calc(100% + 14px)', marginLeft: '6px' }}
                              placeholder="请选择标签"
                              onChange={(e) => this.props.handleChangeTagSelect(e)}
                              value={resource}
                            >
                              {caseInfo.taglist && caseInfo.taglist.length > 0 && caseInfo.taglist.map((item, index) => (
                                <Option key={item}>{item}</Option>
                              ))}
                            </Select>
                          </Col>
                        </Row>
                        {radioValue === '1' && ((selectValue && selectValue.length > 0) || (resource && resource.length > 0)) && (caseInfo.count || caseInfo.count === 0) && <span style={{ marginLeft: 22 }}>{caseInfo.count}条用例集已选</span>}
                      </Radio>
                    </Radio.Group>
                  )}
                </Form.Item>
              }
            </Fragment>
          )}
        </ConfigProvider>
      </Modal>
    );
  }
}
export default Form.create()(TestModal);

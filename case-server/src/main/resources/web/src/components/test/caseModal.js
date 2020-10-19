import React, { Component } from 'react';
import { Modal, Form, Input, Icon, Row, Col, Upload, Select } from 'antd';
const { Dragger } = Upload;
const { TextArea } = Input;
const { Option } = Select;
const formItemLayout = {
  labelCol: { span: 5 },
  wrapperCol: { span: 17 },
};

class CaseModal extends Component {
  constructor(props) {
    super(props);
    this.state = {
      xmindFile: null, // 保存上传的file文件，单文件
    };
  }
  onCloses = () => {
    this.setState({ xmindFile: null });
    this.props.onClose(this.props.form);
  };
  onOk = () => {
    this.setState({ xmindFile: null });
    this.props.handleOk(this.props.form, this.state.xmindFile);
  };
  render() {
    const { visible, options, requirementId } = this.props;
    const { xmindFile } = this.state;
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
    return (
      <Modal
        visible={visible}
        onCancel={() => this.onCloses()}
        onOk={() => this.onOk()}
        title="新增测试用例集"
        okText="确认"
        cancelText="取消"
        width="600px"
        wrapClassName="oe-caseModal-test-wrap"
      >
        <Form.Item {...formItemLayout} label="用例集名称：">
          {getFieldDecorator('case', {
            rules: [{ required: true, message: '请填写用例集名称' }],
            initialValue: '',
          })(<Input placeholder="请填写用例集名称" />)}
        </Form.Item>
        <Form.Item {...formItemLayout} label="关联需求：">
          {getFieldDecorator('requirementId', {
            initialValue: requirementId,
          })(<Input placeholder="所属需求" />)}
        </Form.Item>
        <Form.Item {...formItemLayout} label="描述：">
          {getFieldDecorator('description', {
            initialValue: '',
          })(
            <TextArea
              autoSize={{ minRows: 4 }}
              maxLength="1024"
              placeholder="请填写描述"
            />,
          )}
        </Form.Item>
        <Row style={{ marginBottom: '20px' }}>
          <Col span={5}>导入本地xmind:</Col>
          <Col span={17} className="dragger">
            <div className="flex-child">
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
      </Modal>
    );
  }
}
export default Form.create()(CaseModal);

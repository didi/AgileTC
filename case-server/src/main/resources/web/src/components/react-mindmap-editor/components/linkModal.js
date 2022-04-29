import React from 'react';
import { Modal, Form, Input } from 'antd';

const LinkModal = (props) => {
  const onOk = () => {
    const { form, minder, onCancel } = props;
    form.validateFields((err, values) => {
      if (err) {
        console.log('Received values of form: ', values);
        return;
      }
      const params = { ...values };
      minder.execCommand('HyperLink', params.url, params.title);
      onCancel();
    });
  };
  const defaultObj = props.minder.queryCommandValue('HyperLink');
  const { getFieldDecorator } = props.form;
  return (
    <Modal
      title="链接"
      className="agiletc-modal"
      visible={props.visible}
      onOk={onOk}
      onCancel={props.onCancel}
    >
      <Form layout="vertical">
        <Form.Item label="链接地址">
          {getFieldDecorator('url', {
            rules: [
              {
                required: true,
                message: '必填：以 http(s):// 或 ftp:// 开头',
              },
            ],
            initialValue: defaultObj.url,
          })(<Input placeholder="必填：以 http(s):// 或 ftp:// 开头" />)}
        </Form.Item>
        <Form.Item label="提示文本">
          {getFieldDecorator('title', {
            initialValue: defaultObj.title,
          })(<Input placeholder="选填：鼠标在链接上悬停时提示的文本" />)}
        </Form.Item>
      </Form>
    </Modal>
  );
};
const WrappedLinkForm = Form.create({ name: 'link' })(LinkModal);
export default WrappedLinkForm;

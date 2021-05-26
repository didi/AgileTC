import React from 'react'
import './less/login.less'
import { Form, Input, Button, Icon, message } from 'antd'
import request from '@/utils/axios'
import utils from '@/utils'

class LogIn extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {
      type: '1', // 当前为什么类型 1：登录 2： 注册
      loading: false, // 点击注册登录
    }
  }

  typeChange = type => {
    this.setState({ type }, () => {
      this.props.form.resetFields()
    })
  }

  onOk = () => {
    this.props.form.validateFields((error, value) => {
      if (error) return
      this.setState({ loading: true })
      if (this.state.type === '1') {
        // 登录
        request(`/user/login`, {
          method: 'POST',
          body: { ...value },
        }).then(res => {
          if (res && res.code === 200) {
            message.success('登陆成功')
            window.location.href = utils.getQueryString('jumpto')
          } else {
            message.error(res.msg)
          }
          this.setState({ loading: false })
        })
      } else {
        // 注册
        request(`/user/register`, {
          method: 'POST',
          body: { ...value },
        }).then(res => {
          if (res && res.code === 200) {
            message.success('注册成功')
            window.location.href = utils.getQueryString('jumpto') 
          } else {
            message.error(res.msg)
          }
          this.setState({ loading: false })
        })
      }
    })
  }

  render() {
    const { getFieldDecorator } = this.props.form
    const { type, loading } = this.state
    return (
      <div className="login">
        <div className="card">
          <div className="title">
            AgileTC<span>一套敏捷的测试用例管理平台</span>
          </div>
          <span
            className={type === '1' ? 'btn btn_active' : 'btn'}
            onClick={() => this.typeChange('1')}
          >
            登录
          </span>
          <span
            className={type === '2' ? 'btn btn_active' : 'btn'}
            onClick={() => this.typeChange('2')}
          >
            注册
          </span>
          <div className="input">
            <Form.Item label="">
              {getFieldDecorator('username', {
                rules: [{ required: true, message: '请填写账号' }],
                initialValue: undefined,
              })(<Input placeholder="账号" prefix={<Icon type="user" />} />)}
            </Form.Item>
            {type === '1' && (
              <Form.Item label="">
                {getFieldDecorator('password', {
                  rules: [{ required: true, message: '请填写密码' }],
                  initialValue: undefined,
                })(<Input.Password placeholder="密码" prefix={<Icon type="lock" />} />)}
              </Form.Item>
            )}
            {type === '2' && (
              <Form.Item label="">
                {getFieldDecorator('password', {
                  rules: [{ required: true, message: '请填写密码' }],
                  initialValue: undefined,
                })(<Input.Password placeholder="密码" prefix={<Icon type="lock" />} />)}
              </Form.Item>
            )}
          </div>
          <Button type="primary" className="onBtn" loading={loading} onClick={() => this.onOk()}>
            {type === '1' ? '登录' : '注册并登录'}
          </Button>
        </div>
      </div>
    )
  }
}

export default Form.create()(LogIn)

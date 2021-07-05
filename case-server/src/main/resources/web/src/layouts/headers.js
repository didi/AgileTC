import React from 'react'
import 'antd/dist/antd.css'
import { Layout, Icon, Menu, Dropdown, message } from 'antd'
import getQueryString from '@/utils/getCookies'
import '../pages/landing/less/index.less'
import request from '@/utils/axios'
const { Header } = Layout
const getCookies = getQueryString.getCookie

class Headers extends React.Component {
  componentDidMount() {
    if (!getCookies('username')) {
      window.location.href = `/login?jumpto=${window.location.href}`
    }
  }
  // 登出
  handleDropdownClick = () => {
    request(`/user/quit`, {
      method: 'POST',
    }).then(res => {
      if (res && res.code === 200) {
        window.location.href = `/login?jumpto=${window.location.href}`
      } else {
        message.error(res.msg)
      }
    })
  }

  render() {
    const menu = (
      <Menu className="menu" onClick={this.handleDropdownClick}>
        <Menu.Item key="logout">
          <span>
            <Icon type="logout" />
            退出登录
          </span>
        </Menu.Item>
      </Menu>
    )
    return getCookies('username') ? (
      <Header style={{ zIndex: 9 }}>
        <a href="/" style={{ color: '#fff', fontSize: 24 }}>
          AgileTC
        </a>
        {getCookies('username') ? (
          <Dropdown overlay={menu} overlayClassName="dropStyle" placement="bottomLeft">
            <div className="user">
              <Icon type="user" className="userIcon" />
              <span className="username">{getCookies('username')}</span>
              <Icon type="down" className="dowm" />
            </div>
          </Dropdown>
        ) : (
          <a href="/login" className="loginCss">
            登录/注册
          </a>
        )}
      </Header>
    ) : null
  }
}
export default Headers

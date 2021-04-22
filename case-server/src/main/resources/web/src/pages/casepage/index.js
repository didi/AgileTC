import React from 'react'
import CaseLists from '../../components/case/caselist'
import 'antd/dist/antd.css'
import { Layout, Icon, Menu, Dropdown, message } from 'antd'
import getQueryString from '@/utils/getCookies'
import '../landing/less/index.less'
import request from '@/utils/axios'
const { Header } = Layout
const getCookies = getQueryString.getCookie

class casePage extends React.Component {
  // 登出
  handleDropdownClick = () => {
    request(`/user/quit`, {
      method: 'POST',
    }).then(res => {
      if (res && res.code === 200) {
        window.location.href = '/login'
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
    return (
      <section style={{ marginBottom: 30 }}>
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
        <div style={{ padding: 24 }}>
          <CaseLists
            {...this.props}
            type="oe"
            baseUrl=""
            kityApiPrefix="KITY_dev"
            oeApiPrefix=""
            doneApiPrefix=""
            // oeApiPrefix="api_dev"
            // doneApiPrefix="DONE_dev"
          />
        </div>
      </section>
    )
  }
}
export default casePage

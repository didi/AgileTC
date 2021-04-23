/* eslint no-undef: 0 */
/* eslint arrow-parens: 0 */
import React from 'react'
import { enquireScreen } from 'enquire-js'
import { Layout, Icon, Menu, Dropdown } from 'antd'
import Banner3 from './Banner3'
import Footer0 from './Footer0'
import request from '@/utils/axios'
import getQueryString from '@/utils/getCookies'
const getCookies = getQueryString.getCookie
const { Header } = Layout

import { Banner30DataSource, Footer00DataSource } from './data.source'
import './less/antMotionStyle.less'

let isMobile = {}
enquireScreen(b => {
  isMobile = b
})

const { location = {} } = typeof window !== 'undefined' ? window : {}

export default class Home extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      isMobile,
      show: !location.port, // 如果不是 dva 2.0 请删除
    }
  }

  componentDidMount() {
    // 适配手机屏幕;
    enquireScreen(b => {
      this.setState({ isMobile: !!b })
    })
    // dva 2.0 样式在组件渲染之后动态加载，导致滚动组件不生效；线上不影响；
    /* 如果不是 dva 2.0 请删除 start */
    if (location.port) {
      // 样式 build 时间在 200-300ms 之间;
      setTimeout(() => {
        this.setState({
          show: true,
        })
      }, 500)
    }
    /* 如果不是 dva 2.0 请删除 end */
  }

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
    const children = [
      <Banner3
        id="Banner3_0"
        key="Banner3_0"
        dataSource={Banner30DataSource}
        isMobile={this.state.isMobile}
      />,
      <Footer0
        id="Footer0_0"
        key="Footer0_0"
        dataSource={Footer00DataSource}
        isMobile={this.state.isMobile}
      />,
    ]
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
      <div
        className="templates-wrapper"
        ref={d => {
          this.dom = d
        }}
      >
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
        {/* 如果不是 dva 2.0 替换成 {children} start */}
        {this.state.show && children}
        {/* 如果不是 dva 2.0 替换成 {children} end */}
      </div>
    )
  }
}

import React, { Component } from 'react'
import withRouter from 'umi/withRouter'
import { ConfigProvider, Layout } from 'antd'
import zhCN from 'antd/es/locale/zh_CN'
import { connect } from 'dva'

const { Content } = Layout

function mapStateToProps(state) {
  return {
    global: state.global,
  }
}
class PageLayout extends Component {
  render() {
    const { children = {} } = this.props
    return (
      <ConfigProvider locale={zhCN}>
        <Layout>
          <Content style={{ minHeight: '100vh' }}>{children}</Content>
        </Layout>
      </ConfigProvider>
    )
  }
}
export default withRouter(connect(mapStateToProps)(PageLayout))

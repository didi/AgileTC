import React, { Component } from 'react';
import withRouter from 'umi/withRouter';
import { ConfigProvider, Layout } from 'antd';
import zhCN from 'antd/es/locale/zh_CN';
import { connect } from 'dva';

const { Header, Content } = Layout;

function mapStateToProps(state) {
  return {
    global: state.global,
  };
}
class PageLayout extends Component {
  render() {
    const { props } = this;
    return (
      <ConfigProvider locale={zhCN}>
        <Layout>
          <Header>
            <a href="/" style={{ color: '#fff', fontSize: 24 }}>
              AgileTC
            </a>
          </Header>
          <Content style={{ padding: 24, minHeight: 'calc(100vh - 64px)' }}>
            {props.children}
          </Content>
        </Layout>
      </ConfigProvider>
    );
  }
}
export default withRouter(connect(mapStateToProps)(PageLayout));

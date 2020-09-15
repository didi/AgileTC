import React, { Component } from 'react';
import withRouter from 'umi/withRouter';
import Redirect from 'umi/redirect';
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
    const { children = {} } = this.props;
    const { location = {} } = children.props;
    if (location.hasOwnProperty('key')) {
      return (
        <ConfigProvider locale={zhCN}>
          <Layout>
            <Header>
              <a href="/" style={{ color: '#fff', fontSize: 24 }}>
                AgileTC
              </a>
            </Header>
            <Content style={{ padding: 24, minHeight: 'calc(100vh - 64px)' }}>
              {children}
            </Content>
          </Layout>
        </ConfigProvider>
      );
    }
    return <Redirect to="/case/caseList/1" />;
  }
}
export default withRouter(connect(mapStateToProps)(PageLayout));

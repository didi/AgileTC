import React from 'react'
import CaseLists from '../../components/case/caselist'
import 'antd/dist/antd.css'
import { Layout } from 'antd'
const { Header } = Layout
class casePage extends React.Component {
  render() {
    return (
      <section style={{ marginBottom: 30 }}>
        <Header style={{ zIndex: 9 }}>
          <a href="/" style={{ color: '#fff', fontSize: 24 }}>
            AgileTC
          </a>
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

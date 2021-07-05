import React from 'react'
import CaseLists from '../../components/case/caselist'
import 'antd/dist/antd.css'
import '../landing/less/index.less'
import Headers from '../../layouts/headers'

class casePage extends React.Component {
  render() {
    return (
      <section style={{ marginBottom: 30 }}>
        <Headers />
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

import React from 'react'
import CaseLists from '../../components/case/caselist'
import 'antd/dist/antd.css'
import getQueryString from '@/utils/getCookies'
import '../landing/less/index.less'
import Headers from '../../layouts/headers'
const getCookies = getQueryString.getCookie

class casePage extends React.Component {
  render() {
    return getCookies('username') ? (
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
    ) : null
  }
}
export default casePage

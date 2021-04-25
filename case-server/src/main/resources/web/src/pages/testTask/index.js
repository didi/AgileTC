import React from 'react'
import Casemgt from '../../components/case/casemgt'
import 'antd/dist/antd.css'
import getQueryString from '@/utils/getCookies'
const getCookies = getQueryString.getCookie

class casePage extends React.Component {
  componentDidMount() {
    if (!getCookies('username')) {
      window.location.href = `/login?jumpto=${window.location.href}`
    }
  }
  render() {
    return getCookies('username') ? (
      <section style={{ padding: 24 }}>
        <Casemgt
          {...this.props}
          type="oe"
          baseUrl=""
          kityApiPrefix="KITY_dev"
          oeApiPrefix=""
          doneApiPrefix=""
          // oeApiPrefix="api_dev"
          // doneApiPrefix="DONE_dev"
        />
      </section>
    ) : null
  }
}
export default casePage

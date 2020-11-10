import React from 'react'
import Casemgt from '../../components/case/casemgt'
import 'antd/dist/antd.css'
class casePage extends React.Component {
  render() {
    return (
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
    )
  }
}
export default casePage

import React from 'react';
import CaseLists from '../../components/case/caselist';
import 'antd/dist/antd.css';
class casePage extends React.Component {
  render() {
    return (
      <section style={{ marginBottom: 30 }}>
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
      </section>
    );
  }
}
export default casePage;

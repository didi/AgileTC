import React from 'react';
import Task from '../../components/test';
import 'antd/dist/antd.css';
class testTaskList extends React.Component {
  constructor() {
    super();
    this.state = {
      num: 1,
    };
  }
  addNum = () => {
    let num = this.state.num + 1;
    this.setState({ num });
  };
  render() {
    return (
      <section style={{ marginBottom: 30 }}>
        <Task
          useType="button"
          addNum={this.addNum}
          {...this.props}
          type="oe"
          kityApiPrefix="KITY_dev"
          oeApiPrefix=""
          doneApiPrefix=""
          // oeApiPrefix="api_dev"
          // doneApiPrefix="DONE_dev"
        />
        <Task
          useType="table"
          num={this.state.num}
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
export default testTaskList;

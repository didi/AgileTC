import React, { Component } from 'react';
import { Select } from 'antd';
import { template } from '../constants';

class TemplateGroup extends Component {
  state = {
    templateValue: this.props.minder.queryCommandValue('Template'),
  };
  handleTemplateChange = (templateValue) => {
    const { minder } = this.props;
    minder.execCommand('Template', templateValue);
    this.setState({ templateValue });
  };
  render() {
    const { minder, toolbar = {}, isLock } = this.props;
    let options = [];
    const customTemplate = toolbar.template || Object.keys(template);
    for (let i = 0; i < customTemplate.length; i++) {
      options.push(
        <Select.Option key={customTemplate[i]} value={customTemplate[i]}>
          {template[customTemplate[i]]}
        </Select.Option>
      );
    }
    return (
      <div className="nodes-actions" style={{ width: 140 }}>
        <Select
          dropdownMatchSelectWidth={false}
          getPopupContainer={(triggerNode) => triggerNode.parentNode}
          value={minder.queryCommandValue('Template')}
          onChange={this.handleTemplateChange}
          disabled={isLock}
        >
          {options}
        </Select>
      </div>
    );
  }
}
export default TemplateGroup;

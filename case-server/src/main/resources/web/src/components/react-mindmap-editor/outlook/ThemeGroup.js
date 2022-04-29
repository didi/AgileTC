import React, { Component } from 'react';
import { Select } from 'antd';
import { theme } from '../constants';

class ThemeGroup extends Component {
  state = {
    themeValue: this.props.minder.queryCommandValue('Theme'),
  };
  handleThemeChange = (themeValue) => {
    const { minder } = this.props;
    minder.execCommand('Theme', themeValue);
    this.setState({ themeValue });
  };
  render() {
    const { minder, toolbar = {}, isLock } = this.props;
    const customTheme = toolbar.theme || Object.keys(theme);
    let options = [];
    for (let i = 0; i < customTheme.length; i++) {
      options.push(
        <Select.Option key={customTheme[i]} value={customTheme[i]}>
          {theme[customTheme[i]]}
        </Select.Option>
      );
    }
    return (
      <div className="nodes-actions" style={{ width: 120 }}>
        <Select
          dropdownMatchSelectWidth={false}
          value={minder.queryCommandValue('Theme')}
          onChange={this.handleThemeChange}
          getPopupContainer={(triggerNode) => triggerNode.parentNode}
          disabled={isLock}
        >
          {options}
        </Select>
      </div>
    );
  }
}
export default ThemeGroup;

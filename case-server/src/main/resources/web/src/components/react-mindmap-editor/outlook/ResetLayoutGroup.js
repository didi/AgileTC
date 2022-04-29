import React, { Component } from 'react';
import { Button, Icon } from 'antd';

class ResetLayoutGroup extends Component {
  onClick = () => {
    const { minder } = this.props;
    if (minder.queryCommandState('resetlayout') !== -1) {
      minder.execCommand('resetlayout');
    }
  };
  render() {
    const { isLock } = this.props;
    return (
      <div className="nodes-actions" style={{ width: 72 }}>
        <Button
          type="link"
          size="small"
          className="big-icon"
          disabled={isLock}
          onClick={this.onClick}
        >
          <Icon type="layout" style={{ fontSize: '1.6em' }} />
          <br />
          整理布局
        </Button>
      </div>
    );
  }
}
export default ResetLayoutGroup;

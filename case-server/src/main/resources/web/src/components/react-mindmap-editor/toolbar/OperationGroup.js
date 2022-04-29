import React, { Component } from 'react';
import { Button } from 'antd';

class OperationGroup extends Component {
  render() {
    const { minder, handleShowInput, isLock } = this.props;
    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;
    return (
      <div className="nodes-actions" style={{ width: 64 }}>
        <Button type="link" icon="edit" size="small" disabled={disabled} onClick={handleShowInput}>
          编辑
        </Button>
        <Button
          type="link"
          icon="delete"
          size="small"
          disabled={disabled}
          onClick={() => minder.execCommand('RemoveNode')}
        >
          删除
        </Button>
      </div>
    );
  }
}
export default OperationGroup;

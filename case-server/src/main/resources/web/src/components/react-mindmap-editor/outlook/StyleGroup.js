import React, { Component } from 'react';
import { Button } from 'antd';
import { CustomIcon } from '../components';

class StyleGroup extends Component {
  onClick = (action) => {
    const { minder } = this.props;
    if (minder.queryCommandState(action) !== -1) {
      minder.execCommand(action);
    }
  };
  render() {
    const { minder, isLock } = this.props;
    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;
    const commonStyle = { size: 'small', type: 'link', disabled };
    return (
      <div className="nodes-actions" style={{ width: 164, display: 'flex', alignItems: 'center' }}>
        <Button {...commonStyle} className="big-icon" onClick={() => this.onClick('ClearStyle')}>
          <CustomIcon type="clear" style={{ width: 22, height: 22 }} disabled={disabled} />
          <br />
          清除样式
        </Button>
        <div style={{ width: '50%' }}>
          <Button {...commonStyle} onClick={() => this.onClick('CopyStyle')} icon="copy">
            复制样式
          </Button>
          <br />
          <Button {...commonStyle} onClick={() => this.onClick('pastestyle')}>
            <CustomIcon type="stylePaste" disabled={disabled} />
            粘贴样式
          </Button>
        </div>
      </div>
    );
  }
}
export default StyleGroup;

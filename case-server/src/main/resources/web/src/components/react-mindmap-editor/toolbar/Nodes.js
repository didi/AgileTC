import React, { Component } from 'react';
import { Button } from 'antd';
import { CustomIcon } from '../components';
import './Nodes.scss';

class Nodes extends Component {
  insertNode(type) {
    let { minder } = this.props;
    if (minder) {
      switch (type) {
        case 'childNode':
          minder.queryCommandState('AppendChildNode') === -1 ||
            minder.execCommand('AppendChildNode', '分支主题');
          break;
        case 'parentNode':
          minder.queryCommandState('AppendParentNode') === -1 ||
            minder.execCommand('AppendParentNode', '分支主题');
          break;
        case 'siblingNode':
          minder.queryCommandState('AppendSiblingNode') === -1 ||
            minder.execCommand('AppendSiblingNode', '分支主题');
          break;
        default:
          break;
      }
      this.props.callback();
    }
  }
  render() {
    const { minder, isLock } = this.props;
    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;
    return (
      <div className="nodes-actions" style={{ width: 250 }}>
        <Button
          type="link"
          size="small"
          disabled={disabled}
          onClick={() => {
            this.insertNode('childNode');
          }}
        >
          <CustomIcon type="addChild" disabled={disabled} />
          插入下级主题
        </Button>
        <Button
          type="link"
          size="small"
          disabled={disabled}
          onClick={() => {
            this.insertNode('parentNode');
          }}
        >
          <CustomIcon type="addParent" disabled={disabled} />
          插入上级主题
        </Button>
        <Button
          type="link"
          size="small"
          disabled={disabled}
          onClick={() => {
            this.insertNode('siblingNode');
          }}
        >
          <CustomIcon type="addSibling" disabled={disabled} />
          插入同级主题
        </Button>
      </div>
    );
  }
}
export default Nodes;

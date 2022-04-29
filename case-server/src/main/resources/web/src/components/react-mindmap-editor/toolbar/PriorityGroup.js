import React, { Component } from 'react';
import { Icon, Button, Tooltip } from 'antd';
import './PriorityGroup.scss';

class PriorityGroup extends Component {
  handleAction = (priority) => {
    const { minder } = this.props;
    minder.execCommand('Priority', priority);
  };
  render() {
    const { minder, priority = [1, 2, 3], isLock } = this.props;
    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;
    return (
      <div className="nodes-actions" style={{ width: 120 }}>
        <Tooltip title="移除优先级" getPopupContainer={(triggerNode) => triggerNode.parentNode}>
          <Button
            type="link"
            size="small"
            disabled={disabled}
            style={{ padding: 4, height: 28 }}
            onClick={() => this.handleAction()}
          >
            <Icon
              type="minus-circle"
              theme="filled"
              style={{ fontSize: '18px', color: 'rgba(0, 0, 0, 0.6)' }}
            />
          </Button>
        </Tooltip>
        {priority &&
          priority.map((item) => {
            return (
              <Button
                key={item}
                type="link"
                size="small"
                disabled={disabled}
                className={`priority-btn p${item}`}
                onClick={() => this.handleAction(item)}
              >
                P{item - 1}
              </Button>
            );
          })}
      </div>
    );
  }
}
export default PriorityGroup;

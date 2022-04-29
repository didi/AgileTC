import React, { Component } from 'react';
import { Icon, Button, Tooltip } from 'antd';
import { CustomIcon } from '../components';
import './PriorityGroup.scss';

class ProgressGroup extends Component {
  handleAction = (priority) => {
    const { minder } = this.props;
    minder.execCommand('Progress', priority);
  };

  render() {
    const { minder, isLock } = this.props;
    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;
    const btnProps = {
      type: 'link',
      disabled,
      style: { padding: 4, height: 28 },
    };
    const progressList = [
      {
        label: '移除结果',
        icon: (
          <Icon
            type="minus-circle"
            theme="filled"
            style={{ fontSize: '18px', color: 'rgba(0, 0, 0, 0.6)' }}
          />
        ),
      },
      {
        label: '失败',
        value: 1,
        icon: <CustomIcon type="fail" disabled={disabled} style={{ width: 18, height: 18 }} />,
      },
      {
        label: '通过',
        value: 9,
        icon: <CustomIcon type="checked" disabled={disabled} style={{ width: 18, height: 18 }} />,
      },
      {
        label: '阻塞',
        value: 5,
        icon: <CustomIcon type="block" disabled={disabled} style={{ width: 18, height: 18 }} />,
      },
      {
        label: '不执行',
        value: 4,
        icon: <CustomIcon type="skip" disabled={disabled} style={{ width: 18, height: 18 }} />,
      },
    ];
    return (
      <div className="nodes-actions" style={{ width: 140 }}>
        {progressList &&
          progressList.map((item) => (
            <Tooltip
              key={item.value || 0}
              title={item.label}
              getPopupContainer={(triggerNode) => triggerNode.parentNode}
            >
              <Button {...btnProps} onClick={() => this.handleAction(item.value)}>
                {item.icon}
              </Button>
            </Tooltip>
          ))}
      </div>
    );
  }
}
export default ProgressGroup;

import React, { Component } from 'react';
import { Tag, Input, Icon, message, Tooltip } from 'antd';
import { getUsedResource } from '../util';
import debounce from 'lodash/debounce';

class TagGroup extends Component {
  state = { value: '', selectedResource: [], newTags: [], inputVisible: false, inputValue: '' };
  componentDidMount() {
    let { minder } = this.props;
    minder.on('contentchange selectionchange', () => {
      const selectedResource = minder.queryCommandValue('Resource');
      this.setState({ selectedResource });
    });
  }
  handleClose = (removedTag) => {
    const newTags = this.state.newTags.filter((tag) => tag !== removedTag);
    this.setState({ newTags });
  };
  showInput = () => {
    window.tagInput = true;
    this.setState({ inputVisible: true }, () => this.input.focus());
  };
  handleInputChange = (e) => {
    this.setState({ inputValue: e.target.value });
  };
  handleInputConfirm = () => {
    const { inputValue } = this.state;
    let { newTags } = this.state;
    const { minder } = this.props;
    let tags = getUsedResource(minder.getAllNode());
    if (inputValue && newTags.indexOf(inputValue) === -1 && tags.indexOf(inputValue) === -1) {
      newTags = [...newTags, inputValue];
    } else if (newTags.indexOf(inputValue) !== -1 || tags.indexOf(inputValue) !== -1) {
      message.warning('该标识已存在！');
    }
    this.setState(
      {
        newTags,
        inputVisible: false,
        inputValue: '',
      },
      () => {
        window.tagInput = false;
      }
    );
  };
  saveInputRef = (input) => (this.input = input);
  onAdd = (newResource) => {
    const { minder } = this.props;
    let resource = [];
    const oldResource = minder.queryCommandValue('Resource');
    if (oldResource.indexOf(newResource) < 0) {
      resource = [...oldResource, newResource];
      minder.execCommand('Resource', resource);
    }
  };
  onRemove = (oldResource) => {
    const { minder } = this.props;
    let resource = [];
    const current = minder.queryCommandValue('Resource');
    if (current.indexOf(oldResource) > -1) {
      resource = current.filter((item) => item !== oldResource && item !== null);
      if (resource.length === 0) {
        resource = null;
      }
    }
    minder.execCommand('Resource', resource);
  };
  handleClick = (e, item) => {
    if (!this._delayedClick) {
      this._delayedClick = debounce(this.doClick, 500);
    }
    if (this.clickedOnce) {
      this._delayedClick.cancel();
      this.clickedOnce = false;
      this.onRemove(item);
    } else {
      this._delayedClick(e);
      this.clickedOnce = true;
      this.onAdd(item);
    }
  };
  doClick = () => {
    this.clickedOnce = undefined;
  };
  renderTags = (itemList, closable) => {
    const { minder, isLock } = this.props;
    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;
    return itemList.map((item) => {
      const backgroundColor = minder.getResourceColor(item).toHEX();
      return (
        <Tag
          key={item}
          className={`resource-tag${disabled ? ' disabled' : ''}`}
          disabled={disabled}
          closable={closable}
          color={backgroundColor}
          onClick={(e) => (disabled ? null : this.handleClick(e, item))}
          onClose={() => this.handleClose(item)}
        >
          {item}
        </Tag>
      );
    });
  };
  render() {
    const { newTags, inputVisible, inputValue } = this.state;
    const { minder, tags } = this.props;
    let resourceList = getUsedResource(minder.getAllNode());
    let oldResource = resourceList.filter(
      (item) => newTags.indexOf(item) < 0 && tags.indexOf(item) < 0 && item !== 'undefined'
    );
    return (
      <div className="nodes-actions" style={{ width: 360, padding: '0px 4px' }}>
        <Tooltip
          title="选中节点后，单击添加标签，双击移除标签。"
          getPopupContainer={(triggerNode) => triggerNode.parentNode}
        >
          <div style={{ display: 'inline-block' }}>
            {tags && this.renderTags(tags, false)}
            {oldResource && this.renderTags(oldResource, false)}
            {newTags && this.renderTags(newTags, true)}
          </div>
        </Tooltip>
        {inputVisible && (
          <Input
            ref={this.saveInputRef}
            type="text"
            size="small"
            style={{ width: 78 }}
            value={inputValue}
            onChange={this.handleInputChange}
            onBlur={this.handleInputConfirm}
            onPressEnter={this.handleInputConfirm}
          />
        )}
        {!inputVisible && (
          <Tag onClick={this.showInput} style={{ background: '#fff', borderStyle: 'dashed' }}>
            <Icon type="plus" /> 增加标签
          </Tag>
        )}
      </div>
    );
  }
}
export default TagGroup;

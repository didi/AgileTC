import React, { Component } from 'react';
import { Button, Dropdown, Menu, Icon } from 'antd';
import { LinkModal, ImageModal, NoteDrawer, NoteAddDrawer } from '../components';
import './MediaGroup.scss';

class MediaGroup extends Component {
  state = {
    showLink: false,
    showNote: false,
    showImage: false,
    noteStatus: null,
    drawerVisible: false
  };
  handleLinkMenuClick = ({ key }) => {
    if (key === 'add' || key === 'edit') {
      this.setState({ showLink: true });
    } else {
      this.props.minder.execCommand('HyperLink', null);
    }
  };
  handleImageMenuClick = ({ key }) => {
    if (key === 'add' || key === 'edit') {
      this.setState({ showImage: true });
    } else {
      this.props.minder.execCommand('image', '');
      setTimeout(() => {
        this.props.minder.fire('contentchange');
      }, 100);
    }
  };
  handleNoteMenuClick = ({ key }) => {
    const { minder } = this.props;
    if (key === 'add' || key === 'edit') {
      this.setState({ showNote: true });
    } else {
      minder.execCommand('note', null);
    }
    this.setState({ noteStatus: minder.queryCommandValue('note') });
  };
  renderMenu = (label, key) => {
    const { minder } = this.props;
    const selectedNode = minder.getSelectedNode();
    return (
      <Menu
        onClick={(params) => {
          if (key === 'hyperlink') this.handleLinkMenuClick(params);
          if (key === 'image') this.handleImageMenuClick(params);
          if (key === 'note') this.handleNoteMenuClick(params);
        }}
      >
        <Menu.Item key="add">插入{label}</Menu.Item>
        <Menu.Item key="remove">移除已有{label}</Menu.Item>
        {selectedNode && selectedNode.getData(key) !== undefined && (
          <Menu.Item key="edit">编辑已有{label}</Menu.Item>
        )}
      </Menu>
    );
  };
  render() {
    const {
      minder,
      toolbar = {},
      isLock,
      toolbarCustom = { custom: null, title: '自定义' }
    } = this.props;
    const { showLink, showImage, showNote, noteStatus, drawerVisible } = this.state;

    let disabled = minder.getSelectedNodes().length === 0;
    if (isLock) disabled = true;

    const linkMenu = this.renderMenu('链接', 'hyperlink');
    const imageMenu = this.renderMenu('图片', 'image');
    const noteMenu = this.renderMenu('备注', 'note');
    return (
      <div
        className="nodes-actions"
        style={{ textAlign: 'center', minWidth: 200, paddingRight: 6 }}
      >
        <Dropdown
          overlay={linkMenu}
          trigger={['click']}
          disabled={disabled}
          getPopupContainer={(triggerNode) => triggerNode.parentNode}
        >
          <Button type="link" size="small" className="big-icon">
            <Icon type="link" style={{ fontSize: '1.6em' }} />
            <br />
            链接
            <Icon type="caret-down" />
          </Button>
        </Dropdown>
        {toolbar.image !== false && (
          <Dropdown
            overlay={imageMenu}
            trigger={['click']}
            disabled={disabled}
            getPopupContainer={(triggerNode) => triggerNode.parentNode}
          >
            <Button type="link" size="small" className="big-icon">
              <Icon type="picture" style={{ fontSize: '1.6em' }} />
              <br />
              图片
              <Icon type="caret-down" />
            </Button>
          </Dropdown>
        )}
        <Dropdown
          overlay={noteMenu}
          trigger={['click']}
          disabled={disabled}
          getPopupContainer={(triggerNode) => triggerNode.parentNode}
        >
          <Button type="link" size="small" className="big-icon">
            <Icon type="file-text" style={{ fontSize: '1.6em' }} />
            <br />
            备注
            <Icon type="caret-down" />
          </Button>
        </Dropdown>
        {toolbar.addFactor && (
          <Dropdown
            overlay={
              <Menu onClick={() => this.setState({ drawerVisible: true })}>
                <Menu.Item key="add">用例设计</Menu.Item>
              </Menu>
            }
            trigger={['click']}
            disabled={disabled}
            getPopupContainer={(triggerNode) => triggerNode.parentNode}
          >
            <Button type="link" size="small" className="big-icon" style={{ marginRight: 10 }}>
              <Icon type="tool" style={{ fontSize: '1.6em' }} />
              <br />
              工具
              <Icon type="caret-down" />
            </Button>
          </Dropdown>
        )}
        {toolbarCustom.custom &&
          (!disabled ? (
            toolbarCustom.custom
          ) : (
            <Button type="link" disabled size="small" className="big-icon">
              <Icon type="edit" style={{ fontSize: '1.6em' }} />
              <br />
              {toolbarCustom.title || '自定义'}
            </Button>
          ))}
        {showLink && (
          <LinkModal
            visible={showLink}
            onCancel={() => this.setState({ showLink: false })}
            {...this.props}
            minder={minder}
          />
        )}
        {showImage && (
          <ImageModal
            visible={showImage}
            onCancel={() => this.setState({ showImage: false })}
            {...this.props}
            minder={minder}
          />
        )}
        {showNote && (
          <NoteDrawer
            noteStatus={noteStatus}
            visible={showNote}
            onCancel={() => this.setState({ showNote: false })}
            {...this.props}
            minder={minder}
          />
        )}
        {drawerVisible && (
          <NoteAddDrawer
            visible={drawerVisible}
            onCancel={() => this.setState({ drawerVisible: false })}
            {...this.props}
            minder={minder}
          />
        )}
      </div>
    );
  }
}
export default MediaGroup;

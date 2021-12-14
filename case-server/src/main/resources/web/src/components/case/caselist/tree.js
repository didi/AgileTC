/* eslint-disable */
import React from 'react';
import PropTypes from 'prop-types';
import ResizePanel from 'react-resize-panel';
import _ from 'lodash';
import {
  message,
  Tree,
  Input,
  Tooltip,
  Dropdown,
  Menu,
  Icon,
  Modal,
} from 'antd';
import request from '@/utils/axios';
import './index.scss';
const { TreeNode, DirectoryTree } = Tree;
const { Search } = Input;

class FileTree extends React.Component {
  static propTypes = {
    productLineId: PropTypes.number.isRequired,
    getCaseList: PropTypes.func.isRequired,
    getTreeList: PropTypes.func.isRequired,
    treeData: PropTypes.array.isRequired,
  };
  expandedKeys = [];
  dataList = [];
  constructor() {
    super();
    this.state = {
      treeData: [],
      levelId: '',
      levelText: '',
      expandedKeys: ['1'], //展开树节点
      searchValue: '',
      autoExpandParent: true,
      dataList: [],
      caseIds: [],
      isSelect: true,
      isSibling: true,
      isAdd: true,
      isReName: true,
      treeSelect: null,
    };
  }
  componentDidMount() {
    this.setState({ treeData: this.props.treeData }, () => {
      this.initTreeExpandedKeys(true);
    });
  }
  componentWillReceiveProps(nextProps) {
    if (!_.isEqual(nextProps.treeData, this.props.treeData)) {
      this.setState({ treeData: nextProps.treeData }, () => {
        this.initTreeExpandedKeys(true);
      });
    }
  }
  initTreeExpandedKeys = expandedAll => {
    this.dataList = [];
    const { expandedKeys, treeData } = this.state;
    const dataList = this.generateList(treeData);
    this.setState({
      expandedKeys: expandedAll
        ? dataList
          ? dataList.map(item => item.id)
          : []
        : expandedKeys,
    });
  };
  handleClick = (e, item) => {
    this.setState({ isSelect: false });
    this.originalTreeData = [...this.state.treeData];
    if (e.key == '0') {
      this.addSibling(item);
    } else if (e.key == '1') {
      this.addChild(item);
    } else if (e.key == '2') {
      this.rename(item);
    } else if (e.key == '3') {
      this.isDelete(item);
    }
  };
  addNode = (key, data) => {
    const newTree = data.map(item => {
      const node = { ...item };
      if (key === item.id) {
        const newNode = {
          parentId: node.id,
          id: 1444,
          text: '',
          children: [],
          isEdit: true,
        };
        return { ...item, children: [...item.children, newNode] };
      }
      node.children = this.addNode(key, node.children);
      return node;
    });
    return newTree;
  };

  addSibling = item => {
    const newTree = this.addNode(item.parentId, this.state.treeData);
    this.setState({
      treeData: newTree,
    });
  };

  addChild = item => {
    const { expandedKeys, treeData } = this.state;
    const newTree = this.addNode(item.id, treeData);
    this.setState({
      treeData: newTree,
      expandedKeys: Array.from(new Set([...expandedKeys, item.id])),
    });
  };
  rename = item => {
    this.editNode(item.id, this.state.treeData);
    this.setState({
      treeData: this.state.treeData,
    });
  };
  editNode = (key, data) => {
    if (this.state.isSibling === false) {
      return message.error('请完成当前新增');
    }
    data.map(item => {
      if (item.id === key) {
        item.isEdit = true;
        item.rename = true;
      } else {
        item.isEdit = false;
        item.rename = false;
      }
      if (item.children) {
        this.editNode(key, item.children);
      }
    });
  };
  isDelete = item => {
    if (this.state.isSibling === false) {
      return message.error('请完成当前新增');
    }
    Modal.confirm({
      title: '确认删除文件夹吗',
      content: (
        <span>
          删除&nbsp;&nbsp;<span style={{ color: 'red' }}>{item.text}</span>
          &nbsp;&nbsp;文件夹时，
          <span>
            同时会删除包含的&nbsp;&nbsp;
            <span style={{ color: 'red' }}>{item.caseIds.length}</span>
            &nbsp;&nbsp;个用例集
          </span>
        </span>
      ),
      onOk: e => {
        this.deleteFolder(item);
        Modal.destroyAll();
      },
      icon: <Icon type="exclamation-circle" />,
      cancelText: '取消',
      okText: '删除',
      getContainer:
        document.getElementsByClassName('tc-content')[0] ||
        document.getElementsByClassName('task_card')[0] ||
        document.body,
    });
  };
  deleteFolder = item => {
    let { getTreeList, getCaseList, productLineId, doneApiPrefix } = this.props;
    let url = `${doneApiPrefix}/dir/delete`;
    request(url, {
      method: 'POST',
      body: {
        parentId: item.parentId,
        productLineId,
        delId: item.id,
        channel: 1,
      },
    }).then((res = {}) => {
      if (res.code === 200) {
        this.setState(
          {
            treeSelect: ['root'],
          },
          () => {
            getTreeList(true);
            getCaseList(['root']);
          },
        );
      } else {
        message.error(res.msg);
      }
    });
  };
  nameFiltersInput = (e, value) => {
    this.setState({
      levelId: value.parentId,
      levelText: e.target.value,
    });
  };
  οnblurInput = () => {
    let { getTreeList, productLineId, doneApiPrefix } = this.props;
    let url = `${doneApiPrefix}/dir/add`;
    request(url, {
      method: 'POST',
      body: {
        parentId: this.state.levelId,
        productLineId,
        text: this.state.levelText,
        channel: 1,
      },
    }).then((res = {}) => {
      if (res.code === 200) {
        this.setState({
          levelText: '',
          isSibling: true,
          isAdd: true,
        });
      } else {
        message.error(res.msg);
      }
      getTreeList();
    });
  };
  renameInput = item => {
    if (item.text && this.state.levelText === '') {
      this.state.levelText = item.text;
    }
    if (this.state.levelText === '') {
      return message.error('重命名文件夹名不能为空');
    }
    if (this.state.isReName) {
      this.setState({
        isReName: false,
      });
      setTimeout(() => {
        this.setState({
          isReName: true,
        });
      }, 5000);
    }
    if (this.state.isReName === false) {
      return;
    }
    let { getTreeList, productLineId, doneApiPrefix } = this.props;
    let url = `${doneApiPrefix}/dir/rename`;
    request(url, {
      method: 'POST',
      body: {
        id: item.id,
        productLineId,
        text: this.state.levelText,
        channel: 1,
      },
    }).then((res = {}) => {
      if (res.code === 200) {
        this.setState({
          levelText: '',
        });
        getTreeList();
      } else {
        this.editNode(null, this.state.treeData);
        this.setState({ treeData: this.state.treeData });
        message.error(res.msg);
      }
    });
  };
  renderTreeNodes = data =>
    data.map(item => {
      let { searchValue } = this.state;
      const searchIndex = item.text.indexOf(searchValue);
      const beforeStr = item.text.substr(0, searchIndex);
      const afterStr = item.text.substr(searchIndex + searchValue.length);
      if (item.isEdit) {
        item.title = (
          <div className="titleContainer" onClick={e => e.stopPropagation()}>
            <span>
              <Input
                size="small"
                autoFocus
                defaultValue={item.text}
                style={{ width: '100%' }}
                onChange={e => {
                  this.nameFiltersInput(e, item);
                  e.stopPropagation();
                }}
                onBlur={e => {
                  if (e.target.value === '') {
                    this.setState({ treeData: this.originalTreeData });
                  } else {
                    item.rename ? this.renameInput(item) : this.οnblurInput();
                  }
                }}
                onPressEnter={e => {
                  item.rename ? this.renameInput(item) : this.οnblurInput();
                }}
              />
            </span>
          </div>
        );
      } else {
        item.title = (
          <div className="titleContainer">
            <Tooltip className="tipClass" title={item.text}>
              {searchIndex > -1 ? (
                <div className="item-label">
                  {beforeStr}
                  <span style={{ color: '#f50' }}>{searchValue}</span>
                  {afterStr}
                  <span
                    style={{
                      fontFamily: 'PingFangSC-Regular',
                      fontSize: ' 12px',
                      color: '#8B9ABE',
                      lineHeight: '18px',
                      marginLeft: '2px',
                    }}
                  >
                    ({item.caseIds.length})
                  </span>
                </div>
              ) : (
                <div className="item-label">
                  {item.text}
                  <span
                    style={{
                      fontFamily: 'PingFangSC-Regular',
                      fontSize: ' 12px',
                      color: '#8B9ABE',
                      lineHeight: '18px',
                      marginLeft: '2px',
                    }}
                  >
                    ({item.caseIds.length})
                  </span>
                </div>
              )}
            </Tooltip>
            <span className="iconShow">
              <Dropdown
                overlay={
                  <Menu
                    onClick={e => {
                      this.handleClick(e, item);
                      e.domEvent.stopPropagation();
                    }}
                  >
                    {item.id !== 'root' ? (
                      <Menu.Item key="0">添加同级文件夹</Menu.Item>
                    ) : null}
                    {item.id !== '-1' ? (
                      <Menu.Item key="1">添加子文件夹</Menu.Item>
                    ) : null}
                    {item.id !== '-1' ? (
                      <Menu.Divider style={{ color: '#E0EAFB' }} />
                    ) : null}
                    {item.id !== '-1' ? (
                      <Menu.Item key="2">重命名</Menu.Item>
                    ) : null}
                    {item.id !== '-1' && item.id !== 'root' ? (
                      <Menu.Divider style={{ color: '#E0EAFB' }} />
                    ) : null}
                    {item.id !== '-1' && item.id !== 'root' ? (
                      <Menu.Item key="3">删除</Menu.Item>
                    ) : null}
                  </Menu>
                }
                trigger={['click']}
              >
                <a
                  className="ant-dropdown-link"
                  onClick={e => e.stopPropagation()}
                >
                  <Icon type="dash" style={{ color: '#447CE6' }} />
                </a>
              </Dropdown>
            </span>
          </div>
        );
      }
      if (item.children) {
        return (
          <TreeNode title={item.title} key={item.id} dataRef={item}>
            {this.renderTreeNodes(item.children)}
          </TreeNode>
        );
      }
      return <TreeNode {...item} />;
    });

  generateList = (data, excludeItem = {}) => {
    for (let i = 0; i < data.length; i++) {
      const node = data[i];
      const id = node.id;
      if (excludeItem.id !== node.id) {
        this.dataList.push({ id, text: node.text });
      }
      if (node.children) {
        this.generateList(node.children);
      }
    }
    return this.dataList;
  };
  onChange = e => {
    const value = e.target.value;
    const expandedKeys = this.dataList
      .map(item => {
        if (item.text.indexOf(value) > -1) {
          return this.getParentKey(item.text, this.state.treeData);
        }
        return null;
      })
      .filter((item, i, self) => item && self.indexOf(item) === i);
    this.setState({
      expandedKeys,
      searchValue: value,
      autoExpandParent: true,
    });
  };

  getParentKey = (title, tree) => {
    let parentKey;
    for (let i = 0; i < tree.length; i++) {
      const node = tree[i];
      if (node.children) {
        if (node.children.some(item => item.text === title)) {
          parentKey = node.id;
        } else if (this.getParentKey(title, node.children)) {
          parentKey = this.getParentKey(title, node.children);
        }
      }
    }
    return parentKey;
  };
  onExpand = expandedKeys => {
    this.expandedKeys = expandedKeys;
    this.setState({
      expandedKeys,
      autoExpandParent: false,
    });
  };
  addMenber = caseIds => {
    this.setState({ caseIds }, () => {
      this.props.getCaseList(caseIds);
    });
  };
  onDrop = info => {
    const dropNode = info.node.props;
    const dragNode = info.dragNode.props;
    // console.log(info);
    if (dragNode.eventKey === '-1') {
      message.error('未分类用例集不可移动！');
    } else if (dropNode.eventKey === '-1') {
      message.error('未分类用例集下不可有其他文件！');
    } else {
      const fromId = dragNode.eventKey;
      let toId = '';
      if (dropNode.dragOver) {
        toId = dropNode.eventKey;
      } else {
        toId = dropNode.dataRef.parentId;
      }
      this.moveFolder({
        productLineId: this.props.productLineId,
        fromId,
        toId,
        channel: 1,
      });
    }
  };
  moveFolder = params => {
    const url = `${this.props.doneApiPrefix}/dir/move`;
    request(url, { method: 'POST', body: params }).then((res = {}) => {
      if (res.code === 200) {
        message.success('移动文件夹成功！');
        this.props.getTreeList();
      } else {
        message.error(res.msg);
      }
    });
  };

  render() {
    const { treeSelect, expandedKeys, autoExpandParent, treeData } = this.state;
    return (
      <ResizePanel direction="e" style={{ flexGrow: '1' }}>
        <div className="sidebar">
          <div>
            <Search
              style={{
                paddingRight: '12px',
                marginTop: '16px',
                marginBottom: '12px',
              }}
              placeholder="搜索类别"
              onChange={this.onChange}
            />
            <DirectoryTree
              draggable
              onDrop={this.onDrop}
              multiple
              selectedKeys={treeSelect ? [treeSelect] : []}
              onExpand={this.onExpand}
              expandedKeys={expandedKeys}
              autoExpandParent={autoExpandParent}
              defaultExpandAll
              onSelect={selectedKeys => {
                if (selectedKeys.length > 0) {
                  let newSelect = selectedKeys[0];
                  if (this.state.treeSelect != newSelect) {
                    this.setState({ treeSelect: newSelect }, () => {
                      this.addMenber(selectedKeys);
                    });
                  }
                }
              }}
            >
              {this.renderTreeNodes(treeData)}
            </DirectoryTree>
          </div>
        </div>
      </ResizePanel>
    );
  }
}
export default FileTree;

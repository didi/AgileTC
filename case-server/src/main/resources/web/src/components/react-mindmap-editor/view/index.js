import React, { useState } from 'react';
import { Button, Icon, Dropdown, Menu, Input } from 'antd';
import { CustomIcon } from '../components';
import { expandToList, selectedList } from '../constants';

// 视图tab
const ViewGroup = props => {
  const [selectIndex, setSelectIndex] = useState(0);
  const [searchResult, setSearchResult] = useState(null);
  const [searchText, setSearchText] = useState('');
  const { minder, isLock } = props;
  const handleExpandClick = ({ key }) => {
    minder.execCommand('ExpandToLevel', Number(key));
  };
  const makeBoxActive = () => {
    const $container = document.getElementsByClassName(
      'kityminder-core-container',
    )[0];
    const containerClass = $container.className;
    if (containerClass.indexOf('focus') < 0) {
      $container.className = containerClass + ' focus';
    }
  };
  const handleSelectClick = ({ key }) => {
    let selection = [];
    const selected = minder.getSelectedNodes();
    makeBoxActive();
    switch (key) {
      case 'all':
        minder.getRoot().traverse(node => {
          selection.push(node);
        });
        break;
      case 'revert':
        minder.getRoot().traverse(node => {
          if (selected.indexOf(node) === -1) {
            selection.push(node);
          }
        });
        break;
      case 'siblings':
        selected.forEach(node => {
          if (!node.parent) return;
          node.parent.children.forEach(function(sibling) {
            if (selection.indexOf(sibling) === -1) selection.push(sibling);
          });
        });
        break;
      case 'level':
        const selectedLevel = minder.getSelectedNodes().map(node => {
          return node.getLevel();
        });
        minder.getRoot().traverse(node => {
          if (selectedLevel.indexOf(node.getLevel()) !== -1) {
            selection.push(node);
          }
        });
        break;
      case 'path':
        selected.forEach(function(node) {
          while (node && selection.indexOf(node) === -1) {
            selection.push(node);
            node = node.parent;
          }
        });
        break;
      default:
        minder.getRoot().traverse(node => {
          selection.push(node);
        });
    }
    minder.select(selection, true);
    minder.fire('receiverfocus');
  };
  const generateMenu = list => {
    let menuItems = [];
    for (let key in list) {
      menuItems.push(<Menu.Item key={key}>{list[key]}</Menu.Item>);
    }
    return menuItems;
  };

  const doSearch = (keyword, direction) => {
    minder.fire('hidenoterequest');
    makeBoxActive();

    keyword = keyword.toLowerCase();
    let selection = [];
    const needCompare = ['text', 'note', 'resource'];
    minder.getRoot().traverse(node => {
      const tempItem = node.getData();
      const tempContent = [];
      for (let key in tempItem) {
        if (needCompare.indexOf(key) !== -1) {
          if (typeof tempItem[key] !== 'string') {
            tempContent.push(...tempItem[key]);
          } else tempContent.push(tempItem[key] + '');
        }
      }
      if (tempContent.some(item => item.toLowerCase().indexOf(keyword) > -1)) {
        selection.push(node);
      }
    });
    let _index = selectIndex;
    if (direction === undefined) {
      setSearchResult(selection);
      if (keyword === searchText && (searchResult || []).length > 0) {
        _index = _index < selection.length - 1 ? _index + 1 : 0;
      } else {
        _index = 0;
      }
    } else {
      if (direction === 'prev') {
        _index = _index > 0 ? _index - 1 : selection.length - 1;
      }
      if (direction === 'next') {
        _index = _index < selection.length - 1 ? _index + 1 : 0;
      }
    }
    const node =
      selection.length > 0 ? [selection[_index]] : [minder.getRoot()];
    minder.select(node, true);
    if (!node[0].isExpanded()) minder.execCommand('expand', true);
    setSelectIndex(_index);
  };

  const expandMenu = (
    <Menu onClick={handleExpandClick}>{generateMenu(expandToList)}</Menu>
  );
  const selectedMenu = (
    <Menu onClick={handleSelectClick}>{generateMenu(selectedList)}</Menu>
  );
  return (
    <div className="nodes-actions" style={{ width: '100%' }}>
      <Dropdown
        disabled={isLock}
        overlay={expandMenu}
        getPopupContainer={triggerNode => triggerNode.parentNode}
      >
        <Button type="link" size="small" className="big-icon">
          <Icon type="arrows-alt" style={{ fontSize: '1.6em' }} />
          <br />
          展开 <Icon type="caret-down" />
        </Button>
      </Dropdown>
      <Dropdown
        overlay={selectedMenu}
        getPopupContainer={triggerNode => triggerNode.parentNode}
      >
        <Button type="link" size="small" className="big-icon">
          <CustomIcon type="selectedAll" style={{ width: 22, height: 22 }} />
          <br />
          全选 <Icon type="caret-down" />
        </Button>
      </Dropdown>
      <Input.Search
        className="search-input"
        placeholder="搜索"
        onSearch={value => doSearch(value)}
        style={{ width: 200, marginLeft: 8, marginRight: 8 }}
        value={searchText}
        onChange={e => setSearchText(e.target.value)}
        onFocus={() => {
          window.search = true;
        }}
        onBlur={() => {
          window.search = false;
        }}
      />
      {searchResult && (
        <div style={{ display: 'inline-block' }}>
          <span>
            第{' '}
            {searchResult
              ? searchResult.length === 0
                ? 0
                : selectIndex + 1
              : 0}
            /{searchResult.length} 条
          </span>{' '}
          <Button.Group size="small">
            <Button onClick={() => doSearch(searchText, 'prev')}>
              <Icon type="up" />
            </Button>
            <Button onClick={() => doSearch(searchText, 'next')}>
              <Icon type="down" />
            </Button>
          </Button.Group>
        </div>
      )}
    </div>
  );
};
export default ViewGroup;

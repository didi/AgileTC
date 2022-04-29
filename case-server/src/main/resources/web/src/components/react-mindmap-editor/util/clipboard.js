import MimeType from './mimetype';
import { guid } from '../util';
let beforeCopy = null;
let beforeCut = null;
let beforePaste = null;

const ClipboardRuntime = (minder, readOnly) => {
  let _selectedNodes = [];
  const Data = window.kityminder.data;
  const decode = Data.getRegisterProtocol('json').decode;
  const encode = (nodes) => {
    const kmencode = MimeType.getMimeTypeProtocol('application/km');
    let _nodes = [];
    for (let i = 0, l = nodes.length; i < l; i++) {
      _nodes.push(minder.exportNode(nodes[i]));
    }
    return kmencode(Data.getRegisterProtocol('json').encode(_nodes));
  };
  const isActive = (e) => {
    const hasModal = document.getElementsByClassName('agiletc-modal').length > 0;
    const hasDrawer = document.getElementsByClassName('agiletc-note-drawer').length > 0;
    const hasNotePreviewer = document.getElementsByClassName('note-previewer').length > 0;
    return (
      !hasModal &&
      !hasDrawer &&
      !hasNotePreviewer &&
      !window.showEdit &&
      e.preventDefault &&
      !window.search &&
      !window.tagInput &&
      minder
    );
  };
  beforeCopy = (e) => {
    if (isActive(e)) {
      const clipBoardEvent = e;
      const state = minder.getStatus();

      switch (state) {
        case 'input': {
          break;
        }
        case 'normal': {
          if (!readOnly) {
            const nodes = [].concat(minder.getSelectedNodes());
            if (nodes.length) {
              // 这里由于被粘贴复制的节点的id信息也都一样，故做此算法
              // 这里有个疑问，使用node.getParent()或者node.parent会离奇导致出现非选中节点被渲染成选中节点，因此使用isAncestorOf，而没有使用自行回溯的方式
              if (nodes.length > 1) {
                let targetLevel = null;
                nodes.sort((a, b) => a.getLevel() - b.getLevel());
                targetLevel = nodes[0].getLevel();
                if (targetLevel !== nodes[nodes.length - 1].getLevel()) {
                  let pnode = null;
                  let idx = 0;
                  let l = nodes.length;
                  let pidx = l - 1;

                  pnode = nodes[pidx];

                  while (pnode.getLevel() !== targetLevel) {
                    idx = 0;
                    while (idx < l && nodes[idx].getLevel() === targetLevel) {
                      if (nodes[idx].isAncestorOf(pnode)) {
                        nodes.splice(pidx, 1);
                        break;
                      }
                      idx++;
                    }
                    pidx--;
                    pnode = nodes[pidx];
                  }
                }
              }
              const str = encode(nodes);
              clipBoardEvent.clipboardData.setData('text/plain', str);
            }
          } else {
            const node = minder.getSelectedNode();
            clipBoardEvent.clipboardData.setData('text/plain', node ? node.getText() : '');
          }

          e.preventDefault();
          break;
        }
      }
    }
  };
  beforeCut = (e) => {
    if (isActive(e) && !readOnly) {
      if (minder.getStatus() !== 'normal') {
        e.preventDefault();
        return;
      }

      const clipBoardEvent = e;
      const state = minder.getStatus();

      switch (state) {
        case 'input': {
          break;
        }
        case 'normal': {
          let nodes = minder.getSelectedNodes();
          if (nodes.length) {
            clipBoardEvent.clipboardData.setData('text/plain', encode(nodes));
            minder.execCommand('removenode');
          }
          e.preventDefault();
          break;
        }
      }
    }
  };
  beforePaste = (e) => {
    if (isActive(e) && !readOnly) {
      if (minder.getStatus() !== 'normal') {
        e.preventDefault();
        return;
      }
      const clipBoardEvent = e;
      const textData = clipBoardEvent.clipboardData.getData('text/plain');
      const sNodes = minder.getSelectedNodes();
      const setId = (nodesList, newList) => {
        for (let item of nodesList) {
          const id = guid();
          item.data.id = id;
          item.data.created = new Date().valueOf();
          newList.push({ ...item });
          if (item.children.length > 0) {
            setId(item.children, newList);
          }
        }
      };
      if (MimeType.whichMimeType(textData) === 'application/km') {
        const nodes = decode(MimeType.getPureText(textData));
        let _node = null;
        sNodes.forEach(function (node) {
          // 由于粘贴逻辑中为了排除子节点重新排序导致逆序，因此复制的时候倒过来
          for (let i = nodes.length - 1; i >= 0; i--) {
            nodes[i].data.id = guid();
            nodes[i].data.created = new Date().valueOf();
            let allChildren = [];
            setId(nodes[i].children, allChildren);
            _node = minder.createNode(null, node);
            minder.importNode(_node, nodes[i]);
            _selectedNodes.push(_node);
            node.appendChild(_node);
          }
        });
        minder.select(_selectedNodes, true);
        _selectedNodes = [];
        minder.refresh();
      } else if (
        clipBoardEvent.clipboardData &&
        clipBoardEvent.clipboardData.items[0].type.indexOf('image') > -1
      ) {
        let imageFile = clipBoardEvent.clipboardData.items[0].getAsFile();
        let serverService = angular.element(document.body).injector().get('server');
        return serverService.uploadImage(imageFile).then(function (json) {
          let resp = json.data;
          if (resp.errno === 0) {
            minder.execCommand('image', resp.data.url);
          }
        });
      } else {
        sNodes.forEach(function (node) {
          minder.Text2Children(node, textData);
        });
      }
      e.preventDefault();
    }
  };
  document.addEventListener('copy', beforeCopy);
  document.addEventListener('cut', beforeCut);
  document.addEventListener('paste', beforePaste);
};
const removeListener = () => {
  document.removeEventListener('copy', beforeCopy);
  document.removeEventListener('cut', beforeCut);
  document.removeEventListener('paste', beforePaste);
};
export default { init: ClipboardRuntime, removeListener };

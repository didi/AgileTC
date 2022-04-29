import React, { Component, createRef } from 'react';
import PropTypes from 'prop-types';
import './index.scss';
import 'kity';
import './assets/kityminder-core/kityminder.core.js';
// import Websocket from 'react-websocket';
import Socket from './socket.js';
import {
  ConfigProvider,
  Tabs,
  Input,
  Button,
  Icon,
  notification,
  Modal,
  Spin,
  Switch,
  Tooltip,
  Popover,
  List,
  Avatar,
} from 'antd';
import zhCN from 'antd/es/locale/zh_CN';
import marked from 'marked';
import 'hotbox-ui/hotbox';
import 'hotbox-ui/hotbox.css';
import DoGroup from './toolbar/DoGroup';
import DoMove from './toolbar/DoMove';
import Nodes from './toolbar/Nodes';
import PriorityGroup from './toolbar/PriorityGroup';
import ProgressGroup from './toolbar/ProgressGroup';
import OperationGroup from './toolbar/OperationGroup';
import MediaGroup from './toolbar/MediaGroup';
import TagGroup from './toolbar/TagGroup';
import ThemeGroup from './outlook/ThemeGroup';
import TemplateGroup from './outlook/TemplateGroup';
import ResetLayoutGroup from './outlook/ResetLayoutGroup';
import StyleGroup from './outlook/StyleGroup';
import FontGroup from './outlook/FontGroup';
import ViewGroup from './view';
import { initData, buttons } from './constants';
import { NavBar } from './components';
import { preview, editInput, clipboardRuntime } from './util';

const HotBox = window.HotBox;

const TabPane = Tabs.TabPane;

// 鼠标右键
const MOUSE_RB = 2;

marked.setOptions({
  gfm: true,
  tables: true,
  breaks: true,
  pedantic: false,
  sanitize: true,
  smartLists: true,
  smartypants: false,
});

class KityminderEditor extends Component {
  constructor(props) {
    super(props);
    this.state = {
      minder: null,
      selectedNode: null,
      noteNode: null,
      noteContent: null,
      showEdit: false,
      inputContent: null,
      activeTab: '1',
      showToolBar: this.props.type === 'compare' ? false : true,
      fullScreen: false,
      loading: true,
      isLock: this.props.isLock || false, // 被其他人锁住
      locked: false, // 当前session主动锁住
      popoverVisible: false,
      nowUseList: [],
    };
    this.base = -1;
    this.expectedBase = -1;
    setTimeout(() => {
      if (this.minder) {
        this.setState({
          minder: this.minder,
        });
        clipboardRuntime.init(this.minder, this.props.readOnly);
      }
    }, 100);
    this.navNode = createRef();
  }
  componentDidMount() {
    setTimeout(() => {
      if (!this.props.readOnly) {
        this.arguments = arguments;
        this.initKeyBoardEvent(arguments);
      }
    }, 1000);
    this.initData = initData;
  }
  componentWillUnmount() {
    window.minderData = undefined;
    document.removeEventListener('keydown', this.handleKeyDown);
    clipboardRuntime.removeListener();
    // this.heartCheck.reset();
  }
  getAllData = () => {
    return this.minder.exportJson();
  };
  setEditerData = data => {
    this.minder.importJson(data);
    this.minder.fire('contentchange');
  };
  // 键盘事件的监听
  initKeyBoardEvent = () => {
    setTimeout(() => {
      document.addEventListener('keydown', this.handleKeyDown);
    }, 300);
  };
  initOnEvent = minder => {
    minder.on('import', () => {
      this.setState({ loading: false });
    });
    const { readOnly } = this.props;
    // 视图选中节点变更事件
    minder.on('selectionchange', () => {
      const node = minder.getSelectedNode();
      if (!readOnly) {
        const { selectedNode, showEdit, inputContent } = this.state;
        // 如果被选中节点之前处于编辑状态，变更后节点失去焦点
        // 则先更新节点内容，然后隐藏编辑框、清空编辑框字段
        if (showEdit) {
          if (selectedNode.getText() !== inputContent) {
            selectedNode.setText(inputContent);
            this.minder.refresh();
            this.minder.fire('contentchange');
          }
          this.setState({ showEdit: false, inputContent: null });
          window.showEdit = false;
        }
      }
      this.hotbox.active(HotBox.STATE_IDLE);
      this.setState({
        selectedNode: node,
        noteContent: null,
      });
    });
    if (!readOnly) {
      // 视图双击事件
      minder.on('dblclick', e => {
        // 双击节点，呼出编辑框
        this.handleShowInput();
      });
    }
    minder.on('mousedown', e => {
      if (e.originEvent.button === MOUSE_RB) {
        e.preventDefault();
        const { minder, inputNode } = this;
        if (minder.getSelectedNode() && minder._status !== 'readonly') {
          const node = minder.getSelectedNode();
          const position = editInput(node, inputNode, 'positionOnly');
          setTimeout(() => {
            this.hotbox.active('main', position);
          }, 200);
        }
      }
      this.setState({
        noteContent: null,
      });
    });

    // 视图改变事件的监听，用于持续渲染编辑框的位置
    minder.on('viewchange', e => {
      const { showEdit, noteContent } = this.state;
      const node = minder.getSelectedNode();
      if (showEdit) {
        editInput(node, this.inputNode);
      }
      this.hotbox.active(HotBox.STATE_IDLE);
      if (noteContent !== null) {
        this.handleNotePreview();
      }
    });

    // 备注是否展示的事件监听
    let previewTimer = null;
    minder.on('shownoterequest', e => {
      previewTimer = setTimeout(() => this.handleNotePreview(e), 200);
    });
    minder.on('hidenoterequest', () => {
      clearTimeout(previewTimer);
    });

    minder.on('contentchange', this.sendPatch);
  };
  initHotbox = minder => {
    const {
      priority = [1, 2, 3],
      progressShow = false,
      readOnly = false,
    } = this.props;
    const container = minder.getPaper().container.parentNode;
    const hotbox = new HotBox(container);
    const main = hotbox.state('main');
    if (!readOnly) {
      main.button({
        position: 'center',
        label: '编辑',
        key: 'F2',
        enable: () => !readOnly,
        action: this.handleShowInput,
      });
      buttons.forEach(button => {
        const parts = button.split(':');
        const label = parts.shift();
        const key = parts.shift();
        const command = parts.shift();
        main.button({
          position: 'ring',
          label: label,
          key: key,
          enable: () => !readOnly,
          action: () => {
            if (
              'AppendSiblingNode,AppendChildNode,AppendParentNode'.indexOf(
                command,
              ) > -1
            ) {
              minder.execCommand(command, '分支主题');
              setTimeout(this.handleShowInput, 300);
            } else {
              minder.execCommand(command);
            }
          },
        });
      });
    }

    main.button({
      position: 'top',
      label: '撤销',
      key: 'Ctrl + Z',
      enable: () => {
        if (this.groupNode) {
          return this.groupNode.undo();
        }
        return !readOnly;
      },
      action: () => {
        this.handleUndo();
        // this.groupNode.undo()
      },
      next: 'idle',
    });
    main.button({
      position: 'top',
      label: '重做',
      key: 'Ctrl + Y',
      enable: () => {
        if (this.groupNode) {
          return this.groupNode.redo();
        }
        return !readOnly;
      },
      action: () => {
        this.handleRedo();
        // this.groupNode.redo()
      },
      next: 'idle',
    });
    if (!readOnly) {
      main.button({
        position: 'top',
        label: '优先级',
        key: 'P',
        next: 'priorityBox',
        enable: () => !readOnly,
      });
      const priorityBox = hotbox.state('priorityBox');
      priority.join('').replace(/./g, p => {
        priorityBox.button({
          position: 'ring',
          label: `P${Number(p) - 1}`,
          key: `${Number(p) - 1}`,
          action: () => {
            minder.execCommand('Priority', p);
          },
        });
      });
      priorityBox.button({
        position: 'top',
        label: '移除',
        key: 'Del',
        action: () => {
          minder.execCommand('Priority', 0);
        },
      });
      priorityBox.button({
        position: 'top',
        label: '返回',
        key: 'esc',
        next: 'back',
      });
    }
    if (progressShow) {
      main.button({
        position: 'top',
        label: '结果',
        key: 'G',
        next: 'progress',
        enable: () => progressShow,
      });
      const progress = hotbox.state('progress');
      '1459'.replace(/./g, p => {
        let label = '失败';
        if (p === '4') label = '不执行';
        if (p === '5') label = '阻塞';
        if (p === '9') label = '通过';
        progress.button({
          position: 'ring',
          label,
          key: label,
          action: () => {
            minder.execCommand('Progress', parseInt(p));
          },
        });
      });
      progress.button({
        position: 'top',
        label: '移除',
        key: 'Del',
        action: function() {
          minder.execCommand('Progress', 0);
        },
      });
      progress.button({
        position: 'top',
        label: '返回',
        key: 'esc',
        next: 'back',
      });
    }

    this.hotbox = hotbox;
  };
  // handleUndo = () => {
  //   this.ws.sendMessage('1undo');
  //   const { undoCnt, redoCnt } = this.state;
  //   this.setState({ undoCnt: undoCnt - 1, redoCnt: redoCnt + 1 });
  // };
  // handleRedo = () => {
  //   this.ws.sendMessage('1redo');
  //   const { undoCnt, redoCnt } = this.state;
  //   this.setState({ undoCnt: undoCnt + 1, redoCnt: redoCnt - 1 });
  // };
  handleKeyDown = event => {
    // eslint-disable-next-line
    let e = event || window.event || this.arguments.callee.caller.arguments[0]; //事件
    const ctrlKey = window.event.metaKey || window.event.ctrlKey;
    const hasModal =
      document.getElementsByClassName('agiletc-modal').length > 0;
    const { showEdit, selectedNode, inputContent } = this.state;
    const hasDrawer =
      document.getElementsByClassName('agiletc-note-drawer').length > 0;
    const isRefresh = ctrlKey && window.event.keyCode === 82;
    // comm + s 保存
    if (ctrlKey && window.event.keyCode === 83 && this.props.onSave) {
      e.preventDefault();
      this.props.onSave(this.minder.exportJson());
      // message.info('保存成功！');
    }
    // comm + f 搜索
    if (ctrlKey && window.event.keyCode === 70) {
      e.preventDefault();
      this.setState({ activeTab: '3' });
    }
    if (
      !hasModal &&
      !hasDrawer &&
      !showEdit &&
      e.preventDefault &&
      !isRefresh &&
      !window.search &&
      !window.tagInput
    ) {
      // ctrl + a 全选
      if (ctrlKey && window.event.keyCode === 65) {
        e.preventDefault();
        let selection = [];
        this.minder.getRoot().traverse(node => {
          selection.push(node);
        });
        this.minder.select(selection, true);
      }
      if (this.state.selectedNode !== null) {
        // if (ctrlKey && window.event.keyCode === 67) {
        //   e.preventDefault();
        //   this.minder.execCommand('Copy');
        // }
        // if (ctrlKey && window.event.keyCode === 88) {
        //   e.preventDefault();
        //   this.minder.execCommand('Cut');
        // }
        // if (ctrlKey && window.event.keyCode === 86) {
        //   e.preventDefault();
        //   this.minder.execCommand('Paste');
        // }
        if ([13, 9].indexOf(window.event.keyCode) > -1) {
          const parentClass =
            document.activeElement.parentNode.parentNode.className || '';
          if (
            window.event.keyCode === 13 &&
            parentClass.indexOf('resource-input') < 0
          ) {
            e.preventDefault();
            this.minder.execCommand('AppendSiblingNode', '分支主题');
            setTimeout(this.handleShowInput, 300);
          }
          if (window.event.keyCode === 9) {
            e.preventDefault();
            this.minder.execCommand('AppendChildNode', '分支主题');
            setTimeout(this.handleShowInput, 300);
          }
        }
        if (window.event.keyCode === 8) {
          e.preventDefault();
          this.minder.execCommand('RemoveNode');
        }
      }
      if (this.groupNode) {
        if (ctrlKey && window.event.keyCode === 90) {
          // this.expectedBase = this.minder.getBase() - 1;
          // this.handleUndo();
          e.preventDefault();
          this.groupNode.undo();
        }
        if (ctrlKey && window.event.keyCode === 89) {
          // this.expectedBase = this.minder.getBase() + 1;
          // this.handleRedo();
          e.preventDefault();
          this.groupNode.redo();
        }
      }
    }
    if (showEdit) {
      // 显示编辑框时，shift+回车=换行
      if (window.event.keyCode === 13 && !window.event.shiftKey) {
        e.preventDefault();
        selectedNode.setText(inputContent);
        // this.minder.setStatus('readonly');
        this.minder.refresh();
        // this.minder.setStatus('normal');
        this.minder.fire('contentchange');
        this.setState({ showEdit: false, inputContent: null });
        window.showEdit = false;
      }
    }
  };
  sendPatch = e => {
    if (this.groupNode && window.minderData) {
      this.groupNode.changed();
      const caseObj = e.minder.exportJson();
      caseObj.right = window.minderData.right || 1;

      const patch = this.groupNode.getAndResetPatch();
      if (patch.length === 1 && patch[0].path === '/base') {
          e.minder._status = 'normal';
          return;
      }

      if (patch.length > 0 && e.minder._status !== 'readonly') {
        // if (this.expectedBase !== e.minder.getBase()) {
        //   alert(
        //     `版本信息不对，请刷新页面重试。ExpBase: ${
        //       this.expectedBase
        //     }, base: ${e.minder.getBase()}`,
        //   );
        //   return;
        // }
        // this.ws.sendMessage('1' + JSON.stringify({ case: caseObj, patch }));
        // const { undoCnt } = this.state;
        // this.setState({ undoCnt: undoCnt + 1 });
        // this.websocketHeartbeatJs.send(JSON.stringify({ case: caseObj, patch }));
        this.ws.sendMessage('edit', { caseContent: JSON.stringify(caseObj), patch: JSON.stringify(patch), caseVersion: caseObj.base });
        this.base = e.minder.getBase();
        this.expectedBase = this.base + 1;
        // e.minder.execCommand('resetlayout');
      }
      e.minder._status = 'normal';
    }
  };
  travere = arrPatches => {
    var patches = [];
    for (var i = 0; i < arrPatches.length; i++) {
      if (arrPatches[i].op === undefined) {
        for (var j = 0; j < arrPatches[i].length; j++) {
          patches.push(arrPatches[i][j]);
        }
      } else {
        patches.push(arrPatches[i]);
      }
    }
    return patches;
  };

  handleShowInput = () => {
    const { minder, inputNode } = this;
    if (minder.getSelectedNode() && minder.getStatus() !== 'readonly') {
      const node = minder.getSelectedNode();
      this.setState({ showEdit: true, inputContent: node.getText() }, () => {
        window.showEdit = true;
        editInput(node, inputNode);
        document.getElementsByClassName('edit-input')[0].children[0].select();
      });
    }
  };
  handleInputChange = e => {
    this.setState({ inputContent: e.target.value });
  };
  handleNotePreview = e => {
    if (e) {
      this.setState(
        {
          noteContent: e.node.getData('note')
            ? marked(e.node.getData('note'))
            : '',
          noteNode: e,
        },
        () => {
          preview(e.node, this.previewNode);
        },
      );
    } else {
      this.setState({ noteContent: null });
    }
  };
  handleWsClose = e => {
    // if (this.props.onClose) {
    //   this.props.onClose(this.minder.exportJson());
    // }
    if (this.modal === undefined) {
      this.modal = Modal.warning({
        title: 'Websocket通信已断开，请手动刷新页面。',
        className: 'agiletc-modal ws-warning',
        getContainer: () =>
          document.getElementsByClassName('kityminder-core-container')[0],
        okText: '知道了，立即刷新',
        onOk: () => {
          this.modal = undefined;
          location.reload();
        },
      });
    }
    // this.heartCheck.reset();
  };

  /**
   * 懒绘制大量节点函数
   * @param minder 实例
   * @param data JSON数据
   * @param level 缓加载层数,默认3层
   * @param taskBatchCount 每次执行任务数,默认5次
   */
  largeJsonImport = (minder, data, level = 3, taskBatchCount = 5) => {
    const childrens = [];
    /**
     * 工具函数
     */
    const handleTreeOnDepthWithCallback = (root, depth = 1, cb = () => {}) => {
      if (depth === 0) {
        cb(root);
        return;
      }
      if (root && root.children.length > 0) {
        root.children.forEach(child =>
          handleTreeOnDepthWithCallback(child, depth - 1, cb),
        );
      }
    };
    return new Promise(resolve => {
      let idx = 0;
      handleTreeOnDepthWithCallback(data.root, 3, root => {
        idx++;
        if (root.children && root.children.length > 0) {
          childrens[idx] = JSON.parse(JSON.stringify(root));
          childrens[idx].expandState = 'collapse';
          root.children = [];
        }
      });

      idx = 0;
      const tasks = [];
      const batchRunTask = deadline => {
        while (
          (deadline.timeRemaining() > 0 || deadline.didTimeout) &&
          tasks.length > 0
        ) {
          // 批量执行任务 默认5个一批
          let taskBatchIdx = taskBatchCount;
          while (taskBatchIdx-- && tasks.length) {
            tasks.shift()();
          }
        }

        // 刷新
        minder.refresh(0);
        if (tasks.length > 0) {
          // 需要考虑浏览器兼容，可以考虑pollyfill；
          requestIdleCallback(batchRunTask);
        } else {
          resolve();
        }
      };

      const handleImportBigBundle = () => {
        handleTreeOnDepthWithCallback(minder._root, 3, root => {
          tasks.push(() => {
            idx++;
            const temp = childrens[idx];
            temp !== undefined && minder.importNode(root, temp);
          });
        });
        // 启动渲染
        requestIdleCallback(batchRunTask);

        // 取消动画
        minder.refresh(0);
        // 没有once，手动实现
        minder.off('import', handleImportBigBundle);
      };
      minder.on('import', handleImportBigBundle);
    });
  };

  heartCheck = {
    timeout: 10000, // 毫秒
    timeoutObj: null,
    serverTimeoutObj: null,
    reset: function() {
      clearInterval(this.timeoutObj);
      clearInterval(this.serverTimeoutObj);
      return this;
    },
    start: function(ws) {
      const self = this;
      this.timeoutObj = setTimeout(function() {
        // 这里发送一个心跳，后端收到后，返回一个心跳消息，
        // onmessage拿到返回的心跳就说明连接正常
        ws.sendMessage('0' + 'ping ping ping');
        self.serverTimeoutObj = setTimeout(function() {
          // 如果超过一定时间还没重置，说明后端主动断开了
          // 这里为什么要在send检测消息后，倒计时执行这个代码呢，因为这个代码的目的时为了触发onclose方法，这样才能实现onclose里面的重连方法
          // 所以这个代码也很重要，没有这个方法，有些时候发了定时检测消息给后端，后端超时（我们自己设定的时间）后，不会自动触发onclose方法。
          ws.state.ws.close();
          ws.state.ws.onclose();
        }, self.timeout);
      }, this.timeout);
    },
  };

  handleWsOpen = () => {
    window.ws = this.ws;
    // this.heartCheck.reset().start(this.ws);
    console.log("handle ws open", window.ws);
  };
  handleLock = (data) => {
    switch (data) {
      case '0':
          this.minder.setStatus('readonly');
          this.setState({ isLock: true }, () => {
              document.removeEventListener('keydown', this.handleKeyDown);
          });
          notification.warning({ message: '用例被锁住，当前只读' });
          break;

      case '1':
          this.minder.setStatus('normal', true);
          this.setState({ isLock: false }, () => {
              this.initOnEvent(this.minder);
              document.addEventListener('keydown', this.handleKeyDown);
          });
          notification.warning({ message: '用例被解锁，请刷新重试' });
        break;

      case '2':
          notification.success({ message: '加/解锁成功' });
          this.setState({ locked: !this.state.locked });
        break;

      case '3':
          notification.error({ message: '加/解锁失败' });
          break;
    }
  }

  handleWsUserStat = (data) => {
    console.log( 'user info , ', data);
    this.setState({ nowUseList: data?.split(',') || [] });
  }

  handleWsData = data => {
    // if (data === 'pong pong pong') {
    //   this.heartCheck.reset().start(this.ws);
    //   return;
    // }
    // if (data === 'ping ping ping') {
    //   this.heartCheck.reset().start(this.ws);
    //   this.ws.sendMessage('0' + 'pong pong pong');
    //   return;
    // }
    if (data === 'HTTP_ACCESS_ERROR') {
      notification.warning({ message: '服务端实例退出，请刷新页面。' });
      return;
    }
    if (data === 'websocket on error') {
      this.ws.state.ws.onclose();
      return;
    }

    if (data.substring(0, 4) == '当前用户') {
      // notification.warning({ message: data.split(',')[0] });
      this.setState({ nowUseList: data.split('用户是:')[1]?.split(',') || [] });
      return;
    }
    // if (data.substring(0, 1) == '2') {
    //   // 控制消息
    //   if (data.substring(1, 5) == 'lock') {
    //     this.minder.setStatus('readonly');
    //     this.setState({ isLock: true }, () => {
    //       document.removeEventListener('keydown', this.handleKeyDown);
    //     });
    //     notification.warning({ message: '用例被锁住，当前只读' });
    //   } else if (data.substring(1, 5) == 'unlo') {
    //     this.minder.setStatus('normal', true);
    //     this.setState({ isLock: false }, () => {
    //       this.initOnEvent(this.minder);
    //       document.addEventListener('keydown', this.handleKeyDown);
    //     });
    //     notification.warning({ message: '用例被解锁，请刷新重试' });
    //   } else if (data.substring(1, 5) == 'succ') {
    //     notification.success({ message: '加/解锁成功' });
    //     this.setState({ locked: !this.state.locked });
    //   } else {
    //     notification.error({ message: '加/解锁失败' });
    //   }
    //   return;
    // }
    // const recv;
    try {
      const recv = JSON.parse(data || '{}');

      if (recv.root === undefined) {
        // 如果json解析没有root节点
        this.minder.setStatus('readonly');
        const recvPatches = this.travere(recv);
        const recvBase = recvPatches.filter(item => item.path === '/base')[0]
          ?.value;
        const recvFromBase = recvPatches.filter(
          item => item.path === '/base',
        )[0]?.fromValue;
        this.minder.applyPatches(recvPatches);
        if (!(recvPatches.length === 1)) {
          // 如果是通知报文
          if (recvFromBase != undefined && recvFromBase == recvBase + 1) {
            // undo
            if (this.expectedBase != recvBase + 1) {
              alert(
                `通知报文回复的version错误，需要刷新. Exp:${this.expectedBase},Act:${recvBase}`,
              );
            } else {
              this.expectedBase = recvBase;
            }
          } else {
            if (this.expectedBase != recvBase - 1) {
              alert(
                `通知报文回复的version错误，需要刷新. Exp:${this.expectedBase},Act:${recvBase}`,
              );
            } else {
              this.expectedBase = recvBase;
            }
          }
        } else {
          // 如果是应答报文
          if (this.expectedBase != recvBase) {
            alert(
              `应答报文回复的version错误，需要刷新. Exp:${this.expectedBase},Act:${recvBase}`,
            );
          }
        }
      } else {
        const dataJson = { ...recv };

        // this.largeJsonImport(this.minder, data).then(() => {
        //   // 可以给个右下角的loading标记
        //   // 不需要加遮罩层，界面还是可以操作的
        // });
        if (data === JSON.stringify(this.minder.exportJson())) {
          return;
        }
        window.minderData = undefined;
        this.minder.importJson(dataJson);
        window.minderData = dataJson;

        // 第一次打开用例，预期base与用例的base保持一直
        this.expectedBase = this.minder.getBase();
      }
    } catch (e) {
      console.error(e);
      alert('连接异常，请刷新重试');
    }
  };
  // 随机色
  getColorByName(str) {
    for (
      var i = 0, hash = 0;
      i < str.length;
      hash = str.charCodeAt(i++) + ((hash << 5) - hash)
    );
    let color = Math.floor(
      Math.abs(((Math.sin(hash) * 10000) % 1) * 16777216),
    ).toString(16);
    let rgb = '#' + Array(6 - color.length + 1).join('0') + color;
    return rgb + 'bf';
  }

  onButtonSave = () => {
    this.ws.sendMessage('save', { caseContent: JSON.stringify(this.minder.exportJson()), patch: '', caseVersion: '' });
  }

  onButtonClear = () => {
    this.ws.sendMessage('save', { caseContent: JSON.stringify(this.minder.exportJson()), patch: '', caseVersion: '' });
    this.ws.sendMessage('record_clear', { caseContent: '', patch: '', caseVersion: '' });
  }

  render() {
    const {
      minder,
      noteContent,
      showEdit,
      inputContent,
      noteNode,
      activeTab,
      showToolBar,
      fullScreen,
      loading,
      isLock,
      locked,
      // undoCnt,
      // redoCnt,
      popoverVisible,
      nowUseList,
    } = this.state;
    const {
      progressShow = true,
      readOnly = false,
      editorStyle = {},
      tags,
      wsUrl = '',
      wsParam,
      callback,
      iscore,
      type,
    } = this.props;
    const childProps = {
      ...this.props,
      minder,
      isLock,
    };

    const tabContentClass = `toolbar has-right-border`;

    return (
      <ConfigProvider locale={zhCN}>
        <div
          className={`kityminder-editor-container${
            fullScreen ? ' full-screen' : ''
          }`}
          style={editorStyle}
        >
          {minder && type !== 'compare' && (
            <Socket
              url={wsUrl}
              wsParam={wsParam}
              onOpen={this.handleWsOpen}
              onMessage={this.handleWsData}
              onClose={this.handleWsClose}
              wsMinder={this.minder}
              handleLock={this.handleLock}
              handleWsUserStat={this.handleWsUserStat}
              // onError={e => {
              //   notification.info({
              //     message: 'websocket连接错误，错误详情：' + e,
              //   });
              // }}
              // reconnect={false}
              ref={ws => {
                this.ws = ws;
                window.ws = ws;
              }}
            />
          )}
          {minder && (
            <Tabs
              activeKey={activeTab}
              className={`kityminder-tools-tab${
                showToolBar ? '' : ' collapsed'
              }`}
              tabBarExtraContent={[
                <Popover
                  key="list"
                  placement="bottomRight"
                  content={
                    <List
                      grid={{
                        gutter: 30,
                        column: nowUseList.length >= 4 ? 4 : 1,
                      }}
                      itemLayout="horizontal"
                      dataSource={nowUseList || []}
                      renderItem={item => (
                        <List.Item>
                          <List.Item.Meta
                            avatar={
                              <Avatar
                                style={{
                                  backgroundColor: this.getColorByName(item),
                                }}
                              >
                                {item.substr(0, 1)}
                              </Avatar>
                            }
                            title={
                              <div style={{ lineHeight: '32px' }}>{item}</div>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  }
                  trigger="click"
                  visible={popoverVisible}
                  onVisibleChange={visible =>
                    this.setState({ popoverVisible: visible })
                  }
                >
                  <Tooltip title={'当前在线列表'}>
                    <Button
                      type="primary"
                      style={{ marginRight: 15 }}
                      onClick={() =>
                        this.setState({ popoverVisible: !popoverVisible })
                      }
                    >
                      {nowUseList ? nowUseList.length : 1}人在线
                    </Button>
                  </Tooltip>
                </Popover>,
                <Tooltip
                  key="lock"
                  title={
                    isLock || locked
                      ? '用例被锁住，当前只读，点击开关解锁。'
                      : '用例未上锁，点击开关锁住。'
                  }
                  getPopupContainer={triggerNode => triggerNode.parentNode}
                >
                  {type !== 'compare' && (
                    <Switch
                      size="small"
                      checkedChildren={<Icon type="lock" />}
                      unCheckedChildren={<Icon type="unlock" />}
                      checked={isLock || locked}
                      onClick={checked => {
                        this.ws.sendMessage('lock', {message: checked ? 'lock' : 'unlock'});
                      }}
                      className="agiletc-lock"
                    />
                  )}
                </Tooltip>,
                <Button
                  key="full"
                  type="link"
                  icon={`fullscreen${fullScreen ? '-exit' : ''}`}
                  onClick={() => {
                    this.setState({ fullScreen: !fullScreen });
                    callback && callback({ fullScreen: !fullScreen });
                  }}
                >
                  {fullScreen ? '退出全屏' : '全屏'}
                </Button>,
                <Button
                  key="show"
                  type="link"
                  onClick={() => this.setState({ showToolBar: !showToolBar })}
                >
                  <Icon type="double-left" rotate={showToolBar ? 90 : -90} />{' '}
                  {showToolBar ? '收起' : '展开'}
                </Button>,
              ]}
              onChange={activeKey => {
                this.setState({ activeTab: activeKey });
              }}
            >
              {type !== 'compare' && (
                <TabPane tab="思路" key="1">
                  <div className={tabContentClass}>
                    <DoGroup
                      ref={groupNode => (this.groupNode = groupNode)}
                      initData={this.initData}
                      {...childProps}
                    />
                    {!readOnly && (
                      <Nodes
                        initData={this.initData}
                        {...childProps}
                        callback={() => {
                          setTimeout(this.handleShowInput, 300);
                        }}
                      />
                    )}
                    {!readOnly && <DoMove {...childProps} />}
                    {!readOnly && (
                      <OperationGroup
                        {...childProps}
                        handleShowInput={this.handleShowInput}
                      />
                    )}
                    <MediaGroup {...childProps} />
                    {!readOnly && <PriorityGroup {...childProps} />}
                    {progressShow && <ProgressGroup {...childProps} />}
                    {!readOnly && tags && <TagGroup {...childProps} />}
                  </div>
                </TabPane>
              )}
              <TabPane tab="外观" key={type !== 'compare' ? '2' : '1'}>
                <div className={tabContentClass}>
                  <ThemeGroup {...childProps} />
                  <TemplateGroup {...childProps} />
                  {type !== 'compare' && (
                    <React.Fragment>
                      <ResetLayoutGroup {...childProps} />
                      <StyleGroup {...childProps} />
                      <FontGroup {...childProps} />
                    </React.Fragment>
                  )}
                </div>
              </TabPane>
              {type !== 'compare' && (
                <TabPane tab="视图" key="3">
                  <div className={tabContentClass}>
                    <ViewGroup {...childProps} />
                  </div>
                </TabPane>
              )}
            </Tabs>
          )}
          <div
            className="kityminder-core-container"
            ref={input => {
              if (!this.minder) {
                this.minder = new window.kityminder.Minder({
                  renderTo: input,
                });
                this.centerNode = input;
                this.initOnEvent(this.minder);
                this.initHotbox(this.minder);
              }
            }}
            style={{
              height: `calc(100% - 45px - ${showToolBar ? '80px' : '0px'})`,
            }}
          >
            {loading && <Spin className="agiletc-loader" />}
          </div>
          <NavBar ref={this.navNode} {...childProps} />
          {this.minder && noteContent && (
            <div
              ref={previewNode => (this.previewNode = previewNode)}
              className={`note-previewer${noteContent ? '' : ' hide'}`}
              dangerouslySetInnerHTML={{ __html: noteContent }}
              onMouseEnter={() => this.minder.fire('shownoterequest', noteNode)}
            />
          )}
          {this.minder && (
            <div
              className={`edit-input${showEdit ? '' : ' hide'}`}
              ref={inputNode => (this.inputNode = inputNode)}
            >
              <Input.TextArea
                rows={1}
                value={inputContent || ''}
                onChange={this.handleInputChange}
                onBlur={() => {
                  // minder.setStatus('readonly');
                  minder.refresh();
                  // this.minder.setStatus('normal');
                  minder.fire('contentchange');
                }}
                style={{ minWidth: 300 }}
                autoFocus
                autoSize
              />
            </div>
          )}
        </div>
        <div
            style={{
              display: 'inline-block',
              position: 'fixed',
              bottom: '30px',
              right: '20px',
              zIndex: 999,
            }}
          >
            {iscore != 2 && (
              <Button type="primary" onClick={this.onButtonSave}>
                保存
              </Button>
            )}
            <span> &nbsp; &nbsp;</span>
            {iscore == 3 && (
              <Button type="primary" onClick={this.onButtonClear} >
                清除执行记录
              </Button>
            )}
          </div>
      </ConfigProvider>
    );
  }
}
KityminderEditor.propTypes = {
  priority: PropTypes.any, // priority优先级列表，默认[1,2,3]
  progressShow: PropTypes.any, // 进度toolbar是否显示
  readOnly: PropTypes.any, // 是否只读，不可编辑，不展示toolbar
  tags: PropTypes.any, // 标签列表，没有改属性则工具栏不展示
  toolbar: PropTypes.any, // 工具栏其他设置
  editorStyle: PropTypes.any, // 容器样式
  uploadUrl: PropTypes.any, // 上传请求地址（相对路径）
  wsUrl: PropTypes.any, // websocket请求地址（绝对路径）
  baseUrl: PropTypes.any, // 请求前缀
  onClose: PropTypes.any, // wesocket通信关闭时触发的回调
  onSave: PropTypes.any, // 快捷键保存时触发的事件
  callback: PropTypes.any, // 抛出其他事件，例如全屏
  type: PropTypes.any, // 判断是否为对比历史版本结果页面or查看xmind（只读）
};

export default KityminderEditor;

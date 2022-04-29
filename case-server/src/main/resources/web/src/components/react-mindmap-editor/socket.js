import React from 'react';
import PropTypes from 'prop-types';
import io from './assets/socketio/socket.io.js';
import { notification } from 'antd';
// import { AsyncStorage } from 'react-native-community/async-storage';

class Socket extends React.Component {

    constructor(props) {
        super(props);
        this.state = { ws : io(this.props.url, props.wsParam) };
        this.sendMessage = this.sendMessage.bind(this);
        this.setupSocket = this.setupSocket.bind(this);
        this.leaveListener = this.leaveListener.bind(this);
    }

    setupSocket() {
        var websocket = this.state.ws;

        websocket.on ('connect', () => {
            console.log(this.props);
            if (typeof this.props.onOpen === 'function') this.props.onOpen();
        });

        websocket.on ('reconnect', () => {
            console.log(this.props);
            websocket.disconnect();
            notification.error({ message: 'Version of client is not equal to server, please refresh.'});
        });

        websocket.on('disconnect', () => {
            if (typeof this.props.onClose === 'function') this.props.onClose();
            console.log('disconnect happened.')
            localStorage.setItem(JSON.stringify(this.props.wsParam), JSON.stringify(this.props.wsMinder.exportJson()));
        });

        websocket.on('connect_notify_event', evt => {
            console.log('connect notify ', evt.message);
            if (typeof this.props.handleWsUserStat === 'function') this.props.handleWsUserStat(evt.message);
        });

        websocket.on('open_event', evt => {
            const recv = JSON.parse(evt.message || '{}');
            const dataJson = { ...recv };
            
            try {
                const cacheContent = JSON.parse(localStorage.getItem(JSON.stringify(this.props.wsParam)));
                if (cacheContent == undefined) {
                    throw 'cache is empty'; 
                }
                if (dataJson.base > cacheContent.base) { // 服务端版本高
                    throw 'choose server'; 
                } else { // 客户端版本高 或者 相同
                    // do nothing
                    // websocket.sendMessage('edit', { caseContent: JSON.stringify(cacheContent), patch: null, caseVersion: caseContent.base });
                } 
                window.minderData = undefined;
                this.props.wsMinder.importJson(cacheContent);
                window.minderData = cacheContent;
                this.expectedBase = this.props.wsMinder.getBase();
                console.log('import case from cache. cache base: ', cacheContent.base);
                // todo 测试版本，暂不清除
                localStorage.removeItem(JSON.stringify(this.props.wsParam));
            } catch (e) {
                console.error(e);
                
                console.log('接收消息，data: ', evt.message);
                console.log('接收消息，当前内容: ', JSON.stringify(this.props.wsMinder.exportJson()));
                if (evt.message === JSON.stringify(this.props.wsMinder.exportJson())) {
                  return;
                } 

                window.minderData = undefined;
                this.props.wsMinder.importJson(dataJson);
                window.minderData = dataJson;
        
                // 第一次打开用例，预期base与用例的base保持一直
                this.expectedBase = this.props.wsMinder.getBase();
                console.log('----- 接收消息，expected base: ', this.expectedBase);
                // this.props.onMessage(evt.data);
            }
        });

        websocket.on('edit_ack_event', evt => {
            console.log('edit_ack_event', evt.message);
            const recv = JSON.parse(evt.message || '{}');
            // 如果json解析没有root节点
            this.props.wsMinder.setStatus('readonly');
            const recvPatches = this.travere(recv);
            console.log('====recv=====', evt.message, recv, recvPatches);
            // const recvBase = recvPatches.filter((item) => item.path === '/base')[0]?.value;
            // const recvFromBase = recvPatches.filter((item) => item.path === '/base')[0]?.fromValue;
            try {
                this.props.wsMinder.applyPatches(recvPatches);
            } catch(e) {
                alert('客户端接受应答消息异常，请刷新重试');
            }
            // this.props.wsMinder._status='nomal';
        });

        websocket.on('edit_notify_event', evt => {
            console.log('edit_notify_event', evt.message);
            const recv = JSON.parse(evt.message || '{}');
            // 如果json解析没有root节点
            try {
                this.props.wsMinder.setStatus('readonly');
                const recvPatches = this.travere(recv);
                this.props.wsMinder.applyPatches(recvPatches);
            } catch(e) {
                alert('客户端接受通知消息异常，请刷新重试');
            }
            
        });

        // message 0:加锁；1：解锁；2:加/解锁成功；3:加/解锁失败
        websocket.on('lock', evt => {
            console.log('lock info', evt.message);
            if (typeof this.props.handleLock === 'function') this.props.handleLock(evt.message);
        });

        websocket.on('connect_error', e => {
            console.log('connect_error', e);
            websocket.disconnect();
        });

        websocket.on('warning', e => {
            notification.error({ message: 'server process patch failed, please refresh'});
        });
    }

    travere = (arrPatches) => {
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

    sendMessage(type, message) {
        let websocket = this.state.ws;
        console.log('-- message --', message);
        // var jsonObject = {userName: 'userName', message: message};
        websocket.emit(type, message);
    }

    leaveListener(e) {
        e.preventDefault();
        e.returnValue = '内容将被存储到缓存，下次打开相同用例优先从缓存获取！';
        if (this.props.wsMinder.getBase() > 16) { 
            localStorage.setItem(JSON.stringify(this.props.wsParam), JSON.stringify(this.props.wsMinder.exportJson()));
        }
    }

    componentDidMount() {
        console.log(' -- componentDidMount -- ')
        this.setupSocket();
        window.addEventListener('beforeunload', this.leaveListener);
    }

    componentWillUnmount() {
        window.removeEventListener('beforeunload', this.leaveListener);

        this.state.ws.disconnect();
        console.log(' -- componentWillUnmount -- ');
    }

    render() {
        return (<div></div>)
    }
}

Socket.propTypes = {
    url: PropTypes.string.isRequired,
    onMessage: PropTypes.func.isRequired,
    onOpen: PropTypes.func,
    onClose: PropTypes.func,
    handleLock: PropTypes.func,
    handleWsUserStat: PropTypes.func
}

export default Socket;

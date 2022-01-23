### 心跳
websocket底层基于TCP，是可靠连接，协议层有keepAlive机制来保障连接的可靠性。但是应用层依旧需要有心跳机制。

a) 是保障服务端可以正常响应客户端的请求。对于服务端cpu或内存过高，可能协议层依旧是连接状态，但是服务端实际无法响应客户端请求。

b) 存在一种场景，客户端断开连接后，服务端长时间无法感知，客户端也不知道自身依旧断连，此时如果有心跳机制，可以第一时间发现断连问题。

心跳从客户端还是服务端发起？

网上暂无定论。待讨论。早期的Agiletc是从服务端发起的，当前采用的双向ping的方案。

![image](https://dpubstatic.udache.com/static/dpubimg/9960809b-c0e0-4cb3-8b46-73c9c311c851.png)

### 编辑

![image](https://dpubstatic.udache.com/static/dpubimg/2735bfbd-972f-4b5a-a6cb-2b4913a7bb32.png)

### 保存
刷新或者退出页面，会触发保存。此时的保存场景整体分成2种，1.最后一个编辑者退出，触发的保存；2.非最后一个编辑者退出，触发的保存。 而保存会触发http的保存和ws onclose中的保存逻辑，根据时序的差异，又会有2种场景。2种保存场景都需要考虑时序问题。

- 最后一个编辑者退出，触发的保存

http保存会落库，并备份到backup表。

onclose中的保存会比较数据库中内容和待保存的case content，比较其中的版本号（base值），如果case content版本号更大，则保存，否则不保存。

- 非最后一个编辑者退出，触发的保存

http保存中，比较待保存内容与ws实例中的case content，如果没有差异，则将用例内容落库，并备份到backup表。如果有差异，则计算差异，并且以待保存内容为准，更新ws实例中的case content。并将差异发送给其他客户端。

onclose保存不做保存动作，仅移除player。

### websocket实例关系图
![image](https://dpubstatic.udache.com/static/dpubimg/ea03cf9a-1b85-4276-b4f4-01e959e688e3.png)
package com.xiaoju.framework;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.controller.CaseController;
import com.xiaoju.framework.entity.request.ws.WsSaveReq;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by didi on 2021/11/22.
 */
@ClientEndpoint
public class CaseEditTest extends CaseServerTest {
	private final String url = "ws://127.0.0.1:8094/api/case/2237/undefined/0/yimfeng";
	@Autowired
	private CaseController caseController;
	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(caseController).build();
	}

	@Test
	public void PingPongTest() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			do {
				session.getBasicRemote().sendText("0pong pong pong"); // 发送ping信号
				Thread.sleep(10000);
			} while(true) ;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 编辑测试.测试场景包括(新增节点|删除节点|编辑内容|添加备注|添加图片|添加优先级|添加tag|添加link|undo|redo)
	// 所有编辑操作成对出现.确保内容恢复

	// 新增节点
	@Test
	public void addMessage() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			// 添加数据
			String caseContent1 = "1{\"case\": {\"root\": {\"data\": {\"id\": \"bv8nxhi3c800\",\"created\": 1562059643204,\"text\": \"websocket\"},\"children\": [{\"data\": {\"id\": \"ch3l66s9zlc0\",\"created\": 1641979547721,\"text\": \"分支主题\"},\"children\": []}]},\"template\": \"default\",\"theme\": \"fresh-blue\",\"version\": \"1.4.43\",\"base\": 2,\"right\": 1},\"patch\": [[{\"op\": \"add\",\"path\": \"/root/children/0\",\"value\": {\"data\": {\"id\": \"ch3l66s9zlc0\",\"created\": 1641979547721,\"text\": \"分支主题\"},\"children\": []}}]]}";
			session.getBasicRemote().sendText(caseContent1); // 新增节点分为两步，首先创建一个分支主题
			Thread.sleep(1000);
			String caseContent2 = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"websocket\"},\"children\":[{\"data\":{\"id\":\"ch3lialqzkg0\",\"created\":1641980496404,\"text\":\"新增节点\"},\"children\":[]}]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":3,\"right\":1},\"patch\":[[{\"op\":\"replace\",\"path\":\"/root/children/0/data/text\",\"value\":\"新增节点\"}]]}";
			session.getBasicRemote().sendText(caseContent2); // 修改分支主题的数据
			Thread.sleep(5000);
			session.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// 删除节点
	@Test
	public void deleteMessage() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"websocket\"},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":4,\"right\":1},\"patch\":[[{\"op\":\"remove\",\"path\":\"/root/children/0\"}]]}"; // 将新增节点删除
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// 编辑内容
	@Test
	public void editMessage() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\"},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":5,\"right\":1},\"patch\":[[{\"op\":\"replace\",\"path\":\"/root/data/text\",\"value\":\"case\"}]]}";  // 将websocket修改为case
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// 添加备注
	@Test
	public void addRemarks() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent1 = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\",\"note\":\"# test\\n\"},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":6,\"right\":1},\"patch\":[[{\"op\":\"add\",\"path\":\"/root/data/note\",\"value\":\"# test\\n\"}]]}";
			session.getBasicRemote().sendText(caseContent1);
			Thread.sleep(5000);
			String caseContent2 = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\",\"note\":\"# test\\n1111\"},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":10,\"right\":1},\"patch\":[[{\"op\":\"replace\",\"path\":\"/root/data/note\",\"value\":\"# test\\n1111\"}]]}";
			session.getBasicRemote().sendText(caseContent2);
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// 添加图片
	@Test
	public void addPicture() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\",\"note\":\"# test\\n1111\",\"image\":\"http://localhost:8094/2022/01/13/1511cca7-b9b0-4a54-b99e-7fe45a906dc5.png\",\"imageSize\":{\"width\":200,\"height\":142}},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":7,\"right\":1},\"patch\":[[{\"op\":\"add\",\"path\":\"/root/data/image\",\"value\":\"http://localhost:8094/2022/01/13/1511cca7-b9b0-4a54-b99e-7fe45a906dc5.png\"},{\"op\":\"add\",\"path\":\"/root/data/imageSize\",\"value\":{\"width\":200,\"height\":142}}]]}";
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(5000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// 添加优先级
	@Test
	public void addPriority() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\",\"note\":\"# test\\n1111\",\"image\":\"http://localhost:8094/2022/01/13/1511cca7-b9b0-4a54-b99e-7fe45a906dc5.png\",\"imageSize\":{\"width\":200,\"height\":142},\"priority\":1},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":8,\"right\":1},\"patch\":[[{\"op\":\"replace\",\"path\":\"/root/data/priority\",\"value\":1}]]}";
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(5000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	// 添加标签
	@Test
	public void addTags() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\",\"note\":\"# test\\n1111\",\"image\":\"http://localhost:8094/2022/01/13/1511cca7-b9b0-4a54-b99e-7fe45a906dc5.png\",\"imageSize\":{\"width\":200,\"height\":142},\"priority\":1,\"resource\":[\"前置条件\"]},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":9,\"right\":1},\"patch\":[[{\"op\":\"add\",\"path\":\"/root/data/resource\",\"value\":[\"前置条件\"]}]]}";
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(5000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 添加link
	@Test
	public void addLink() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\",\"note\":\"# test\\n1111\",\"image\":\"http://localhost:8094/2022/01/13/1511cca7-b9b0-4a54-b99e-7fe45a906dc5.png\",\"imageSize\":{\"width\":200,\"height\":142},\"priority\":1,\"resource\":[\"前置条件\"],\"hyperlink\":\"www.baidu.com\"},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":10,\"right\":1},\"patch\":[[{\"op\":\"add\",\"path\":\"/root/data/hyperlink\",\"value\":\"www.baidu.com\"}]]}";
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// redo操作
	@Test
	public void redo() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "1redo";
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// undo操作
	@Test
	public void undo() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent1 = "1{\"case\":{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"case\"},\"children\":[]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":5,\"right\":1},\"patch\":[[{\"op\":\"replace\",\"path\":\"/root/data/text\",\"value\":\"case\"}]]}";  // 将websocket修改为case
			session.getBasicRemote().sendText(caseContent1); // 发送删除消息
			String caseContent2 = "1undo";
			Thread.sleep(5000);
			session.getBasicRemote().sendText(caseContent2); // 发送undo消息
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	// 控制测试.测试场景包括(lock)
	@Test
	public void CaseControllTest() {
		Session session = null;
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			URI uri = URI.create(url);
			session = container.connectToServer(this, uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			String caseContent = "2lock";
			session.getBasicRemote().sendText(caseContent);
			Thread.sleep(10000);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

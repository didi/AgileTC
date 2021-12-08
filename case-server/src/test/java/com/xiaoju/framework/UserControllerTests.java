package com.xiaoju.framework;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.controller.UserController;
import com.xiaoju.framework.entity.request.auth.UserLoginReq;
import com.xiaoju.framework.entity.request.auth.UserRegisterReq;
import com.xiaoju.framework.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


/**
 * Created by yimfeng on 2021/11/22.
 */
public class UserControllerTests extends CaseServerTest {
	@Autowired
	private UserController userController;
	private UserService userService;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
	}


	@Test
	public void exportTest1() throws Exception {

		request = new MockHttpServletRequest();
		request.setCharacterEncoding("utf-8");
		response = new MockHttpServletResponse();


		// 测试register接口 post方法
		UserRegisterReq userRegisterReq = new UserRegisterReq();

		userRegisterReq.setUsername("nimeng5");
		userRegisterReq.setPassword("123456");


		MvcResult mvcResultRegister = mockMvc.perform(MockMvcRequestBuilders.post("/api/user/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(userRegisterReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		System.out.print(mvcResultRegister);

		// 测试login接口 post方法
		UserLoginReq userLoginReq = new UserLoginReq();
		userLoginReq.setUsername("nimeng5");
		userLoginReq.setPassword("123456");


		MvcResult mvcResultLogin = mockMvc.perform(MockMvcRequestBuilders.post("/api/user/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(userLoginReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();
		System.out.print(mvcResultLogin);


		// 注销注册的用户
		userController.logoff("nimeng5");
	}

}

package com.xiaoju.framework;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.controller.DirController;
import com.xiaoju.framework.entity.request.dir.DirCreateReq;
import com.xiaoju.framework.entity.request.dir.DirDeleteReq;
import com.xiaoju.framework.entity.request.dir.DirRenameReq;
import com.xiaoju.framework.service.DirService;
import org.dom4j.Element;
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


/**
 * Created by yimfeng on 2021/11/22.
 */
public class DirControllerTests extends CaseServerTest {
	@Autowired
	private DirController dirController;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(dirController).build();
	}

	@Test
	public void exportTest1() throws Exception {

		// 测试list接口，get方法
		MvcResult mvcResultList = mockMvc.perform(MockMvcRequestBuilders.get("/api/dir/list")
				.accept(MediaType.APPLICATION_JSON).param("productLineId", "1")
				.accept(MediaType.APPLICATION_JSON).param("channel", "1"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentList = mvcResultList.getResponse().getContentAsString();
		JSONObject jsonList = JSONObject.parseObject(contentList);
		System.out.println("data中的内容为 : " + jsonList.getString("data"));

 		// 测试add接口, post方法
		DirCreateReq dirCreateReq = new DirCreateReq("root", 1L, "测试节点", 1);

		MvcResult mvcResultAdd = mockMvc.perform(MockMvcRequestBuilders.post("/api/dir/add")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(dirCreateReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentAdd = mvcResultAdd.getResponse().getContentAsString();
		JSONObject jsonAdd = JSONObject.parseObject(contentAdd);
		System.out.println("data中的内容为 : " + jsonAdd.getString("data"));


		// 通过其他信息获得id
		String id = dirController.getId("root", 1L, 1, "测试节点");
		System.out.println("id的数据为：" + id);

		// 测试rename接口, post方法
		DirRenameReq dirRenameReq = new DirRenameReq(id, 1L, "1234567", 1);

		MvcResult mvcResultRename = mockMvc.perform(MockMvcRequestBuilders.post("/api/dir/rename")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(dirRenameReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();


		String contentRename = mvcResultRename.getResponse().getContentAsString();
		JSONObject jsonRename = JSONObject.parseObject(contentRename);
		System.out.println("data中的内容为 : " + jsonRename.getString("data"));


		// 测试delete接口,post方法
		DirDeleteReq dirDeleteReq = new DirDeleteReq("root", 1L, id, 1);

		MvcResult mvcResultDelete = mockMvc.perform(MockMvcRequestBuilders.post("/api/dir/delete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(dirDeleteReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();


		String contentDelete = mvcResultDelete.getResponse().getContentAsString();
		JSONObject jsonDelete = JSONObject.parseObject(contentDelete);
		System.out.println("data中的内容为 : " + jsonDelete.getString("data"));

		// 测试cardTree接口，get方法
		dirController.getDirTree(1L, 1);
		MvcResult mvcResultDirTree = mockMvc.perform(MockMvcRequestBuilders.get("/api/dir/cardTree")
				.accept(MediaType.APPLICATION_JSON).param("productLineId", "1")
				.accept(MediaType.APPLICATION_JSON).param("channel", "1"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentDirTree = mvcResultDirTree.getResponse().getContentAsString();
		JSONObject jsonDirTree = JSONObject.parseObject(contentDirTree);
		System.out.println("data中的内容为 : " + jsonDirTree.getString("data"));
	}
}
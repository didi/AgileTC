package com.xiaoju.framework;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.controller.RecordController;
import com.xiaoju.framework.entity.request.record.RecordAddReq;
import com.xiaoju.framework.entity.request.record.RecordDeleteReq;
import com.xiaoju.framework.entity.request.record.RecordUpdateReq;
import com.xiaoju.framework.entity.request.ws.RecordWsClearReq;
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
public class RecordControllerTests extends CaseServerTest {

	@Autowired
	private RecordController RecordController;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(RecordController).build();
	}

	@Test
	public void exportTest1() throws Exception {

		// 测试list接口 get方法
		MvcResult mvcResultList = mockMvc.perform(MockMvcRequestBuilders.get("/api/record/list")
				.accept(MediaType.APPLICATION_JSON).param("caseId", "2212"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentList = mvcResultList.getResponse().getContentAsString();
		JSONObject jsonList = JSONObject.parseObject(contentList);
		System.out.println("data中的内容为 : " + jsonList.getString("data"));


		// 测试create接口 post方法
		RecordAddReq recordAddReq = new RecordAddReq();
		recordAddReq.setCaseId(2494L);
		recordAddReq.setCreator("yimfeng");
		recordAddReq.setTitle("测试record123");
		recordAddReq.setChooseContent("{\"priority\":[\"0\"],\"resource\":[]}");
		recordAddReq.setDescription("这是一个测试");
		recordAddReq.setExpectStartTime(1635696000000L);
		recordAddReq.setExpectEndTime(1638374399999L);

		MvcResult mvcResultCreate = mockMvc.perform(MockMvcRequestBuilders.post("/api/record/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(recordAddReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty()) // 查看是否正常update
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentCreate = mvcResultCreate.getResponse().getContentAsString();
		JSONObject jsonCreate = JSONObject.parseObject(contentCreate);
		System.out.println("data中的内容为 : " + jsonCreate.getString("data"));



		// 测试edit接口 post方法
		RecordUpdateReq recordUpdateReq = new RecordUpdateReq();
		recordUpdateReq.setId(967L);
		recordUpdateReq.setModifier("yimfeng");
		recordUpdateReq.setOwner("yimfeng");
		recordUpdateReq.setTitle("测试record1111111");
		recordUpdateReq.setChooseContent("{\"priority\":[\"0\"],\"resource\":[]}");
		recordUpdateReq.setDescription("这是一个测试1");
		recordUpdateReq.setExpectStartTime(1635696000000L);
		recordUpdateReq.setExpectEndTime(1638374399999L);

		MvcResult mvcResultEdit = mockMvc.perform(MockMvcRequestBuilders.post("/api/record/edit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(recordUpdateReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();


		// 测试delete接口 post方法
		RecordDeleteReq recordDeleteReq = new RecordDeleteReq();
		recordDeleteReq.setId(964L);
		MvcResult mvcResultDelete = mockMvc.perform(MockMvcRequestBuilders.post("/api/record/delete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(recordDeleteReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value("删除成功"))
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentDelete = mvcResultDelete.getResponse().getContentAsString();
		JSONObject jsonDelete = JSONObject.parseObject(contentDelete);
		System.out.println("data中的内容为 : " + jsonDelete.getString("data"));


		// 测试clear接口 post方法
		RecordWsClearReq recordWsClearReq = new RecordWsClearReq();
		recordWsClearReq.setId(965L);
		recordWsClearReq.setModifier("yimfeng");
		MvcResult mvcResultClear = mockMvc.perform(MockMvcRequestBuilders.post("/api/record/clear")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(recordWsClearReq)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentClear = mvcResultClear.getResponse().getContentAsString();
		JSONObject jsonClear = JSONObject.parseObject(contentClear);
		System.out.println("data中的内容为 : " + jsonClear.getString("data"));


		// 测试getrecordInfo接口 get方法
		MvcResult mvcResultInfo = mockMvc.perform(MockMvcRequestBuilders.get("/api/record/getRecordInfo")
				.accept(MediaType.APPLICATION_JSON).param("id", "966"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentInfo = mvcResultInfo.getResponse().getContentAsString();
		JSONObject jsonInfo = JSONObject.parseObject(contentInfo);
		System.out.println("data中的内容为 : " + jsonInfo.getString("data"));
	}

}

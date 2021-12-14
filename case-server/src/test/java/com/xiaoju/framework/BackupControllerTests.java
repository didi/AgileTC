package com.xiaoju.framework;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.controller.BackupController;
import com.xiaoju.framework.controller.UploadController;
import com.xiaoju.framework.entity.persistent.CaseBackup;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;


/**
 * Created by yimfeng on 2021/11/22.
 */
public class BackupControllerTests extends CaseServerTest {
	@Autowired
	private BackupController backupController;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(backupController).build();
	}

	@Test
	public void exportTest1() throws Exception {

		// 测试add接口 post方法
		CaseBackup caseBackup = new CaseBackup();
		caseBackup.setId(1635L);
		caseBackup.setCaseId(2654L);
		caseBackup.setCreator("yimfeng");
		caseBackup.setCaseContent("{\"root\":{\"data\":{\"id\":\"bv8nxhi3c800\",\"created\":1562059643204,\"text\":\"中心主题\"},\"children\":[{\"data\":{\"id\":\"cg9k2vx2cr40\",\"created\":1638929669164,\"text\":\"111\"},\"children\":[]}]},\"template\":\"default\",\"theme\":\"fresh-blue\",\"version\":\"1.4.43\",\"base\":3,\"right\":1}");
		caseBackup.setTitle("离开自动保存用例");
		caseBackup.setExtra("");
		caseBackup.setGmtCreated(new Date());
		caseBackup.setIsDelete(0);
		caseBackup.setRecordContent("");

		MvcResult mvcResultadd = mockMvc.perform(MockMvcRequestBuilders.post("/api/backup/add")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSONObject.toJSONString(caseBackup)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentAdd = mvcResultadd.getResponse().getContentAsString();
		JSONObject jsonAdd = JSONObject.parseObject(contentAdd);
		System.out.println("data的内容为 : " + jsonAdd.getString("data"));

		// 测试getBackupByCaseId接口 get方法
		MvcResult mvcResultGet = mockMvc.perform(MockMvcRequestBuilders.get("/api/backup/getBackupByCaseId")
				.accept(MediaType.APPLICATION_JSON).param("caseId", "2637")
				.accept(MediaType.APPLICATION_JSON).param("beginTime", "1635696000000")
				.accept(MediaType.APPLICATION_JSON).param("endTime", "2638374399999"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentGet = mvcResultGet.getResponse().getContentAsString();
		JSONObject jsonGet = JSONObject.parseObject(contentGet);
		System.out.println("data的内容为 : " + jsonGet.getString("data"));

		// 测试getCaseDiff接口 get方法
		MvcResult mvcResultDiff = mockMvc.perform(MockMvcRequestBuilders.get("/api/backup/getCaseDiff")
				.accept(MediaType.APPLICATION_JSON).param("caseId1", "1606")
				.accept(MediaType.APPLICATION_JSON).param("caseId2", "1608"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentDiff = mvcResultDiff.getResponse().getContentAsString();
		JSONObject jsonDiff = JSONObject.parseObject(contentDiff);
		System.out.println("data的内容为 : " + jsonDiff.getString("data"));


		// 测试deleteByCaseId接口 get方法
		MvcResult mvcResultDelete = mockMvc.perform(MockMvcRequestBuilders.get("/api/backup/deleteByCaseId")
				.accept(MediaType.APPLICATION_JSON).param("caseId", "2637"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		String contentDelete = mvcResultDelete.getResponse().getContentAsString();
		JSONObject jsonDelete = JSONObject.parseObject(contentDelete);
		System.out.println("data的内容为 : " + jsonDelete.getString("data"));

	}

}

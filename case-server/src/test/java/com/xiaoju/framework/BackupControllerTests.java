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

		// 测试getBackupByCaseId接口 get方法
		backupController.getBackupByCaseId(2481L, "beginTime", "endTime");
		MvcResult mvcResultGet = mockMvc.perform(MockMvcRequestBuilders.get("/api/backup/getBackupByCaseId")
				.accept(MediaType.APPLICATION_JSON).param("caseId", "2481")
				.accept(MediaType.APPLICATION_JSON).param("beginTime", "1635696000000")
				.accept(MediaType.APPLICATION_JSON).param("endTime", "1638374399999"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();
		System.out.print(mvcResultGet);

		// 测试getCaseDiff接口 get方法
		backupController.getCaseDiff(738L, 736L);
		MvcResult mvcResultDiff = mockMvc.perform(MockMvcRequestBuilders.get("/api/backup/getCaseDiff")
				.accept(MediaType.APPLICATION_JSON).param("caseId1", "738")
				.accept(MediaType.APPLICATION_JSON).param("caseId2", "736"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();
		System.out.print(mvcResultDiff);

		// 测试deleteByCaseId接口 get方法
		backupController.deleteByCaseId(738L);
		MvcResult mvcResultDelete = mockMvc.perform(MockMvcRequestBuilders.get("/api/backup/deleteByCaseId")
				.accept(MediaType.APPLICATION_JSON).param("caseId", "738"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();
		System.out.print(mvcResultDelete);

		// 测试add接口
		CaseBackup caseBackup = new CaseBackup();
		caseBackup.setId(1599L);
		caseBackup.setCaseId(2620L);
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
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		System.out.println(mvcResultadd);

	}

}

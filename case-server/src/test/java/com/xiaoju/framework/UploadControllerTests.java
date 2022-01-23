package com.xiaoju.framework;

import com.xiaoju.framework.controller.CaseController;
import com.xiaoju.framework.controller.UploadController;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.io.File;
import java.io.FileInputStream;

/**
 * Created by didi on 2021/11/19.
 */
public class UploadControllerTests extends CaseServerTest {
	@Autowired
	private UploadController uploadController;
	private CaseController caseController;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(uploadController).build();
	}

	@Test
	public void exportTest1() throws Exception {
		request = new MockHttpServletRequest();
		request.setServerPort(8094);
		request.setCharacterEncoding("utf-8");
		response = new MockHttpServletResponse();

		// 测试import接口 post方法
		File file = new File("/Users/didi/Desktop/picture.xmind");
		MockMultipartFile multipartFile = new MockMultipartFile(
				"file", // 需要对应接口中的MultipartFile file参数的名字
				"picture.xmind", // 文件的名字
				null, //文件类型
				new FileInputStream(file) //文件流
		);

		uploadController.importXmind(multipartFile, "yimfeng", "-1", 1L, "done测试1", "测试", 1, "123", request);


		// 测试uploadAttachment接口 post方法
		File picfile = new File("/Users/didi/Desktop/sql.png");
		MockMultipartFile picMultipartFile = new MockMultipartFile(
				"file",
				"sql.png",
				ContentType.APPLICATION_OCTET_STREAM.toString(), //文件类型
				new FileInputStream(picfile) //文件流
		);

		uploadController.uploadAttachment(picMultipartFile, request);
		MvcResult mvcResultUpload = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/file/uploadAttachment")
				.file(picMultipartFile))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isNotEmpty())
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
				.andDo(MockMvcResultHandlers.print())
				.andReturn();

		System.out.print(mvcResultUpload);


		// 测试export接口 get方法
		uploadController.exportXmind(2223l, request, response);
		System.out.print(response);
		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/file/export")
				.accept(MediaType.APPLICATION_JSON).param("id", "2223"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andReturn();
		System.out.print(mvcResult);
	}
}

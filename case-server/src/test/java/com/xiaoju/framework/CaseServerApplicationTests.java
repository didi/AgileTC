package com.xiaoju.framework;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.util.TreeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.HashSet;

@SpringBootTest
@Slf4j
public class CaseServerApplicationTests {

	@Test
	public void contextLoads() {

		String json = "{\"template\":\"default\",\"root\":{\"data\":{\"expandState\":\"expand\",\"resource\":[\"需求用例\",\"任务用例\"],\"created\":1562059643204,\"background\":\"#fda1ff\",\"id\":\"bv8nxhi3c800\",\"text\":\"P0核心case1188\"},\"children\":[{\"data\":{\"note\":\"111\",\"image\":\"\",\"expandState\":\"expand\",\"resource\":[\"任务用例\"],\"created\":1594636691184,\"id\":\"c45fj2qo8f40\",\"text\":\"分支主题lllrrr\",\"imageSize\":\"\",\"imageTitle\":\"\",\"priority\":2},\"children\":[{\"data\":{\"created\":1594880722099,\"id\":\"c47u14zu89s0\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"expandState\":\"expand\",\"created\":1594648933290,\"id\":\"c45jvaopq2o0\",\"text\":\"aaa\\nwqwqw\"},\"children\":[{\"data\":{\"created\":1594880725130,\"id\":\"c47u16dyqf40\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"expandState\":\"expand\",\"created\":1594644522609,\"id\":\"c45ib0g3zts0\",\"text\":\"分支主题ee222ee\"},\"children\":[]}]},{\"data\":{\"expandState\":\"expand\",\"created\":1594644291831,\"id\":\"c45i82fh6w00\",\"text\":\"分支主题rrr\"},\"children\":[]},{\"data\":{\"created\":1594880650687,\"id\":\"c47u086tins0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"expandState\":\"expand\",\"created\":1594646318409,\"id\":\"c45ixxfd1cw0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"expandState\":\"expand\",\"created\":1594647073190,\"id\":\"c45j7k62ejk0\",\"text\":\"yoyoeeee\"},\"children\":[{\"data\":{\"created\":1594717382175,\"id\":\"c4684ro4wiw0\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"resource\":[\"任务用例\"],\"created\":1594720212569,\"id\":\"c4694vxo33k0\",\"text\":\"分支主题ff333r33322\\n333\",\"priority\":1},\"children\":[{\"data\":{\"resource\":[\"需求用例\"],\"created\":1594720388661,\"id\":\"c46974twkuw0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1594880990089,\"id\":\"c47u4k3wn2o0\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":1594808181890,\"id\":\"c474bgheck00\",\"text\":\"分00支主题\"},\"children\":[{\"data\":{\"created\":1594880908526,\"id\":\"c47u3in000w0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1594880965434,\"id\":\"c47u48s5ej40\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1594880965434,\"id\":\"c47u48s5ej40\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":1594880908526,\"id\":\"c47u3in000w0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1594880995665,\"id\":\"c47u4mo4fbs0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1594880995665,\"id\":\"c47u4mo4fbs0\",\"text\":\"分支主题\"},\"children\":[]}]}]},{\"data\":{\"created\":1594880961154,\"id\":\"c47u46tdfmw0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1594808147271,\"id\":\"c474b0kvgt40\",\"text\":\"分555支主题\"},\"children\":[]},{\"data\":{\"created\":1594880780044,\"id\":\"c47u1vm4xi80\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1594880784286,\"id\":\"c47u1xkad7k0\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":1594880788168,\"id\":\"c47u1zchwxc0\",\"text\":\"分支主题\"},\"children\":[{\"data\":{\"created\":1594880808162,\"id\":\"c47u28j5hh40\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":1594880700797,\"id\":\"c47u0v7j6nc0\",\"text\":\"分支主题\"},\"children\":[]}]},{\"data\":{\"created\":1594880707689,\"id\":\"c47u0yditk00\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1594880711154,\"id\":\"c47u0zytva80\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"created\":1594717433539,\"id\":\"c4685f9lqqo0\",\"text\":\"分支主题\"},\"children\":[]},{\"data\":{\"expandState\":\"expand\",\"created\":1594645258695,\"id\":\"c45ikelmxgg0\",\"text\":\"分支主题\"},\"children\":[]}]},\"theme\":\"fresh-blue\",\"right\":4,\"version\":\"1.4.43\",\"base\":161}";
		JSONObject jsonObject = JSON.parseObject(json);
		HashSet<String> set = new HashSet<>();
		set.add("需求用例");
		TreeUtil.getChosenCase(jsonObject.getJSONObject("root"), set,"resource");
		System.out.println(jsonObject.toString());
		System.out.println(set);

		System.out.println(TreeUtil.getCaseNum(jsonObject.getJSONObject("root"), new HashSet<>()));
	}

}

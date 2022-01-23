package com.xiaoju.framework;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@WebAppConfiguration
public class CaseServerTest {
	@Before
	public void init() {
		System.out.println("start");
	}

	@After
	public void after() {
		System.out.println("end");
	}
}

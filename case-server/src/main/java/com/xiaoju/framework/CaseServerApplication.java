package com.xiaoju.framework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 启动类
 *
 * @author didi
 * @date 2020/11/26
 */
@ServletComponentScan
@SpringBootApplication
public class CaseServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CaseServerApplication.class, args);
	}

}

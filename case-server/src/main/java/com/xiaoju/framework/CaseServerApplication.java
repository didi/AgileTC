package com.xiaoju.framework;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;

import org.apache.tomcat.websocket.server.WsSci;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@MapperScan("com.xiaoju.framework.mapper")
@EnableSwagger2
public class CaseServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CaseServerApplication.class, args);
	}

	/**
	 //     * 配置一个TomcatEmbeddedServletContainerFactory bean
	 //     *http重定向到https
	 //     * @return
	 //     */
	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory(){
			@Override
			protected void postProcessContext(Context context){
				// 如果要强制使用https，请松开以下注释
//            SecurityConstraint securityConstraint = new SecurityConstraint();
//            securityConstraint.setUserConst raint("CONFIDENTIAL");
//            SecurityCollection collection = new SecurityCollection();
//            collection.addPattern("/*");
//            securityConstraint.addCollection(collection);
//            context.addConstraint(securityConstraint);
			}
		};
		tomcat.addAdditionalTomcatConnectors(createStandardConnector());
		return tomcat;
	}

	/*
	 * 让我们的应用支持HTTP是个好想法，但是需要重定向到HTTPS，
	 * 但是不能同时在application.properties中同时配置两个connector， 所以要以编程的方式配置HTTP
	 * connector，然后重定向到HTTPS connector
	 *
	 * @return Connector
	 */
	private Connector createStandardConnector() {
		// 默认协议为org.apache.coyote.http11.Http11NioProtocol
		Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
		connector.setSecure(false);
		connector.setScheme("http");
		connector.setPort(port);
		connector.setRedirectPort(httpsPort); // 当http重定向到https时的https端口号
		return connector;
	}

	@Value("${http.port}")
	private Integer port;

	@Value("${server.port}")
	private Integer httpsPort;

	/**
	 * 创建wss协议接口
	 * @return
	 */
	@Bean
	public TomcatContextCustomizer tomcatContextCustomizer() {
		System.out.println("TOMCATCONTEXTCUSTOMIZER INITILIZED");
		return new TomcatContextCustomizer() {
			@Override
			public void customize(Context context) {
				context.addServletContainerInitializer(new WsSci(), null);
			}

		};
	}

}

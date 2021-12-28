package com.xiaoju.framework.config;

import com.xiaoju.framework.handler.Room;
import com.xiaoju.framework.handler.WebSocket;
import com.xiaoju.framework.mapper.CaseBackupMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.CaseBackupService;
import com.xiaoju.framework.service.RecordService;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.websocket.server.WsSci;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


/**
 * 配置类
 *
 * @author didi
 * @date 2020/11/26
 */
@MapperScan("com.xiaoju.framework.mapper")
@Configuration
public class ApplicationConfig {

    /**
     * http的端口
     */
    @Value("${http.port}")
    private Integer port;

    /**
     * https的端口
     */
    @Value("${server.port}")
    private Integer httpsPort;

    /**
     * tomcat用于找到被注解ServerEndpoint包裹的类
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 配置一个TomcatEmbeddedServletContainerFactory bean
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory(){
            @Override
            protected void postProcessContext(Context context){
                // 如果要强制使用https，请松开以下注释
                // SecurityConstraint securityConstraint = new SecurityConstraint();
                // securityConstraint.setUserConstraint("CONFIDENTIAL");
                // SecurityCollection collection = new SecurityCollection();
                // collection.addPattern("/*");
                // securityConstraint.addCollection(collection);
                // context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        return tomcat;
    }

    /**
     * 让我们的应用支持HTTP是个好想法，但是需要重定向到HTTPS，
     * 但是不能同时在application.properties中同时配置两个connector， 所以要以编程的方式配置HTTP
     * connector，然后重定向到HTTPS connector
     */
    private Connector createStandardConnector() {
        // 默认协议为org.apache.coyote.http11.Http11NioProtocol
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setSecure(false);
        connector.setScheme("http");
        connector.setPort(port);
        connector.setRedirectPort(httpsPort);
        return connector;
    }

    /**
     * 创建wss协议接口
     */
    @Bean
    public TomcatContextCustomizer tomcatContextCustomizer() {
        return context -> context.addServletContainerInitializer(new WsSci(), null);
    }

    /**
     * 给{@code WebSocket}的注入依赖
     * 你可能会发现WebSocket已经有了{@code Component}，为什么不使用{@code Resource}或者{@code Autowired}
     * 原因如下：
     * 因为@EndPointServer注解劫持了WebSocket的实例，这里就把Bean的管理权交给了tomcat，tomcat利用反射给每个线程生成每一个websocket实例
     * 通过这样的方式进行线程隔离，所以{@code WebSocket}下所有的this.xxx看起来应该是会有线程问题，其实本质上并不会因为多个请求而互相干扰
     * 所以两个依赖加上了static，然后通过这样的方式注入，表示两个依赖跟着{@code WebSocket}这个.class类型进入了方法区，而不是跟着实例进堆
     *
     * 感兴趣可以在websocket类中的方法打断点，进来一个请求去追寻方法栈
     * @see org.apache.tomcat.websocket.pojo.PojoEndpointServer#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
     * 这个函数当中的
     * pojo = sec.getConfigurator().getEndpointInstance(sec.getEndpointClass());
     * 会生成一个默认初始化处理器org.apache.tomcat.websocket.server.DefaultServerEndpointConfigurator
     * 然后在函数public <T> T getEndpointInstance(Class<T> clazz)下调用return clazz.getConstructor().newInstance();
     * 完成基于反射的实例构造
     */
    @Autowired
    public void setWebsocketService(RecordService recordService, TestCaseMapper caseMapper, CaseBackupService caseBackupService) {
//        WebSocket.recordService = recordService;
//        WebSocket.caseMapper = caseMapper;
        Room.caseMapper = caseMapper;
        Room.recordService = recordService;
        Room.caseBackupService = caseBackupService;
    }
}

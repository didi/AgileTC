package com.xiaoju.framework.config;

import com.xiaoju.framework.handler.WebSocket;
import com.xiaoju.framework.service.ExecRecordService;
import com.xiaoju.framework.service.WebSocketService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Enumeration;

import feign.RequestInterceptor;

/**
 * Created by didi on 2019/9/23.
 */
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Autowired
//    @Qualifier("WebSocketService")
    public void setMessageService(WebSocketService webSocketService, ExecRecordService execRecordService) {
        WebSocket.webSocketService = webSocketService;
        WebSocket.execRecordService = execRecordService;
    }

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    /**
     * feign调用其他服务时，补充header信息传递
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                return;
            }

            boolean flag = true;
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    Enumeration<String> values = request.getHeaders(name);
                    while (values.hasMoreElements()) {
                        String value = values.nextElement();
                        //如果已有sessionId不用再重复设置
                        if (name.equalsIgnoreCase("cookie") && value.matches("(?i)session=.*"))
                            flag = false;
                        requestTemplate.header(name, value);
                    }
                }
            }

            if (!flag)
                return;

            //解决客户端首次访问时，header中无session id的情况
            String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
            if (StringUtils.isNotBlank(sessionId)) {
                try {
                    requestTemplate.header("cookie", "SESSION=" + new String(Base64.getEncoder().encode(sessionId.getBytes("UTF-8")), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

        };
    }


    /**
     * 增加SpringMVC 日期转换类
     */
    @PostConstruct
    public void initEditableAvlidation() {
        ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) handlerAdapter.getWebBindingInitializer();
        if (initializer.getConversionService() != null) {
            GenericConversionService genericConversionService = (GenericConversionService) initializer.getConversionService();
//            genericConversionService.addConverter(new DateConverter());//添加自定义的类型转换器
        }
    }
}

package com.xiaoju.framework.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.apache.http.impl.client.HttpClients.custom;

/**
 * @author jiangxia
 * @date 2020-03-24
 * @description
 */
@Slf4j
public class HttpUtils {
    private static final int HTTP_RESPONSE_OK = 200;

    /**
     * HttpClient 连接池
     */
    private static PoolingHttpClientConnectionManager cm = null;
    private static CloseableHttpClient httpClient = null;


    static {
        // 初始化连接池，可用于请求HTTP
        cm = new PoolingHttpClientConnectionManager();
        // 整个连接池最大连接数
        cm.setMaxTotal(2000);
        cm.setDefaultMaxPerRoute(20);
        httpClient = custom().setConnectionManager(cm).build();
    }

    public static String doPost(String url, Map<String, Object> param, Integer timeout){
        URI uri = null;
        try {
            uri = buildUri(url, param);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setConfig(defaultRequestConfig(timeout));

        return doHttp(httpPost);
    }

    public static String doPost(String url, String json, Integer timeout){
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(defaultRequestConfig(timeout));

        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);

        return doHttp(httpPost);
    }

    public static String doGet(String url, Map<String, Object> param, Map<String, String> header,
                               Integer timeout)
            throws IOException {
        URI uri = null;
        try {
            uri = buildUri(url, param);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet httpGet = new HttpGet(uri);
        httpGet.setConfig(defaultRequestConfig(timeout));
        setHeader(httpGet, header);

        return doHttp(httpGet);

    }

    static URI buildUri(String url, Map<String, Object> param) throws URISyntaxException {
        // 创建uri
        URIBuilder builder = new URIBuilder(url);
        if (param != null) {
            for (String key : param.keySet()) {
                builder.addParameter(key, param.get(key).toString());
            }
        }
        return builder.build();
    }

    private static void setHeader(HttpRequestBase httpRequestBase, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        headers.entrySet().stream()
                .forEach(entry -> httpRequestBase.setHeader(entry.getKey(), entry.getValue()));
    }

    /**
     * 请求超时设置
     * @param timeout
     * @return
     */
    static RequestConfig defaultRequestConfig(Integer timeout) {
        return RequestConfig.custom().setConnectTimeout(2000)
                .setSocketTimeout((Objects.nonNull(timeout) && timeout > 0) ? timeout : 10000)
                .build();
    }

    static String doHttp(HttpRequestBase request){
        // 通过连接池获取连接对象
        return doRequest(httpClient, request);
    }

    private static String doRequest(CloseableHttpClient httpClient, HttpRequestBase request){
        String result = null;
        CloseableHttpResponse response = null;
        try {
            // 获取请求结果
            response = httpClient.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status == HTTP_RESPONSE_OK) {
                HttpEntity entity = response.getEntity();
                result =
                        entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8.name())
                                : null;
            }
        }  catch (Exception e) {
            log.error("post exception, url is:" + request.getURI() + "and exception is:" + e.getMessage());
            return "";
        } finally {
            if (null != response) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        return result;
    }

    public static String post(String url, String jsonBody) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONNECTION, "close");
        try {
            StringEntity entity = new StringEntity(jsonBody, "utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
        } catch (Exception e) {
            log.error("UnsupportedEncodingException");
            return "";
        }

        String response = null;
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK && /*主要是为了兼容jenkins stop build返回302*/httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
                return "";
            }
            response = EntityUtils.toString(httpEntity, "utf-8");
        } catch (Exception e) {
            log.error("post exception, url is:" + url + ",and jsonBody is:" + jsonBody + ", and exception is:" + e.getMessage());
            return "";
        } finally {
            log.debug("post releaseConnection end!");
            httpPost.releaseConnection();

            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("response=" + response);
        }
        return response;
    }

}

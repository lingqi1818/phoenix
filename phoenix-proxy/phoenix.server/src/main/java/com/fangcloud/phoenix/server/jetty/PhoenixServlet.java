package com.fangcloud.phoenix.server.jetty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.fangcloud.phoenix.server.connection.ProviderAddressPool;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;

public class PhoenixServlet extends HttpServlet {

    /**
     * 
     */
    private static final long                  serialVersionUID = 1L;
    private static final Logger                LOGGER           = Logger
            .getLogger(PhoenixServlet.class);
    private AsyncHttpClient                    httpClient;
    private ProviderAddressPool                providerAddressPool;
    private PoolingHttpClientConnectionManager poolConnManager;

    public void init() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.INSTANCE).build();
        poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // Increase max total connection to 200  
        poolConnManager.setMaxTotal(200);
        // Increase default max connection per route to 20  
        poolConnManager.setDefaultMaxPerRoute(20);
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(5000).build();
        poolConnManager.setDefaultSocketConfig(socketConfig);

        List<String> services = new ArrayList<String>();
        services.add("com.fangcloud.phoenix.test.HelloService");
        providerAddressPool = ProviderAddressPool.makePool("127.0.0.1:2181", services);
        AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
        configBuilder.setConnectTimeout(5000);
        configBuilder.setReadTimeout(5000);
        configBuilder.setRequestTimeout(5000);
        this.httpClient = new AsyncHttpClient(configBuilder.build());
    }

    //    private CloseableHttpClient getConnection() {
    //        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(5000)
    //                .setConnectTimeout(5000).setSocketTimeout(5000).build();
    //        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(poolConnManager)
    //                .setDefaultRequestConfig(requestConfig).build();
    //        if (poolConnManager != null && poolConnManager.getTotalStats() != null) {
    //        }
    //        return httpClient;
    //    }

    public void service(HttpServletRequest request, final HttpServletResponse response) {
        // try {
        String uri = request.getRequestURI();
        String serviceName = uri.substring(1);
        //            CloseableHttpClient closeableHttpClient = getConnection();
        //            HttpPost post = new HttpPost(
        //                    "http://" + providerAddressPool.getAddressByServiceName(serviceName) + uri);
        //            post.setEntity(new InputStreamEntity(request.getInputStream()));
        //            makeHeader(post, request);
        //            HttpResponse resp = closeableHttpClient.execute(post);
        //            response.setHeader("Transfer-Encoding",
        //                    resp.getFirstHeader("Transfer-Encoding").getValue());
        //            response.setHeader("Server", resp.getFirstHeader("Server").getValue());
        //            response.getOutputStream().write(EntityUtils.toByteArray(resp.getEntity()));
        //        } catch (Exception ex) {
        //            ex.printStackTrace();
        //        }
        try {
            BoundRequestBuilder builder = httpClient.preparePost(
                    "http://" + providerAddressPool.getAddressByServiceName(serviceName) + uri);
            makeHeader(builder, request);
            builder.setBody(request.getInputStream());
            ListenableFuture<Boolean> future = builder.execute(new AsyncHandler<Boolean>() {

                @Override
                public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart body)
                        throws Exception {
                    write(response, body.getBodyPartBytes());
                    return com.ning.http.client.AsyncHandler.STATE.CONTINUE;
                }

                @Override
                public Boolean onCompleted() throws Exception {
                    return true;
                }

                @Override
                public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers)
                        throws Exception {
                    FluentCaseInsensitiveStringsMap headerMap = headers.getHeaders();
                    if (headerMap != null) {
                        Set<String> keys = headerMap.keySet();
                        for (String key : keys) {
                            response.setHeader(key, headerMap.getFirstValue(key));
                        }
                    }
                    return com.ning.http.client.AsyncHandler.STATE.CONTINUE;
                }

                @Override
                public com.ning.http.client.AsyncHandler.STATE onStatusReceived(HttpResponseStatus status)
                        throws Exception {
                    return com.ning.http.client.AsyncHandler.STATE.CONTINUE;
                }

                @Override
                public void onThrowable(Throwable t) {
                    LOGGER.error("proxy request failed:", t);
                }
            });

            Boolean result = future.get(5000, TimeUnit.MICROSECONDS);
            if (result == null || !result) {
                writeError(response, "send to dubbo failed !");
            }
        } catch (Exception e) {
            String msg = "proxy request failed," + e.getMessage();
            writeError(response, msg);

            LOGGER.error("proxy request failed:", e);
        }
    }

    private void write(HttpServletResponse response, byte[] array) {
        try {
            response.getOutputStream().write(array);
        } catch (IOException e) {
            LOGGER.error("write  failed:", e);
        }
    }

    private void writeError(HttpServletResponse response, String msg) {
        try {
            response.getOutputStream().write(msg.getBytes());
            response.getOutputStream().flush();
        } catch (IOException e) {
            LOGGER.error("write error failed:", e);
        }
    }

    private void makeHeader(BoundRequestBuilder builder, HttpServletRequest request) {
        //        Enumeration<String> names = request.getHeaderNames();
        //        while (names.hasMoreElements()) {
        //            String name = names.nextElement();
        //            String value = request.getHeader(name);
        builder.setHeader("Content-Type", request.getHeader("Content-Type"));
        builder.setHeader("Accept-Encoding", request.getHeader("Accept-Encoding"));
        builder.setHeader("Host", request.getHeader("Host"));
        builder.setHeader("Accept", request.getHeader("Accept"));
        builder.setHeader("Connection", request.getHeader("Connection"));
        // builder.setHeader("Content-Length", request.getHeader("Content-Length"));
        //}
    }

    //    private void makeHeader(HttpPost post, HttpServletRequest request) {
    //        post.setHeader("Content-Type", request.getHeader("Content-Type"));
    //        post.setHeader("Accept-Encoding", request.getHeader("Accept-Encoding"));
    //        post.setHeader("Host", request.getHeader("Host"));
    //        post.setHeader("Accept", request.getHeader("Accept"));
    //        post.setHeader("Connection", request.getHeader("Connection"));
    //        //post.setHeader("Content-Length", request.getHeader("Content-Length"));
    //    }

}

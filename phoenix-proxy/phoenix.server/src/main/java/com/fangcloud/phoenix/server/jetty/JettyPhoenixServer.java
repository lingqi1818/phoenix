package com.fangcloud.phoenix.server.jetty;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.fangcloud.phoenix.server.PhoenixServer;

/**
 * 基于jetty和httpclient的实现，httpclient的连接池是并发连接数，非真正连接池，性能会差些
 * 
 * @author chenke
 * @date 2017年4月1日 下午2:33:45
 */
public class JettyPhoenixServer implements PhoenixServer {
    private static final Logger LOGGER = Logger.getLogger(JettyPhoenixServer.class);
    private Server              server;

    @Override
    public void start() {
        server = new Server(9898);
        initServer();
        applyServlet();
        try {
            server.start();
            LOGGER.info("start Phoenix-server success !");
            server.join();
        } catch (Exception e) {
            LOGGER.error("start server failed:", e);
        }
    }

    private void applyServlet() {
        HandlerCollection hc = new HandlerCollection();
        ServletHandler sh = new ServletHandler();
        ResourceHandler rh = new ResourceHandler();
        rh.setDirectoriesListed(false);
        rh.setWelcomeFiles(new String[] { "index.html" });
        sh.addServletWithMapping(new ServletHolder(PhoenixServlet.class), "/*");
        hc.addHandler(sh);
        hc.addHandler(rh);
        server.setHandler(hc);

    }

    private void initServer() {
        SelectChannelConnector sc = (SelectChannelConnector) server.getConnectors()[0];
        sc.setAcceptors(1);
        sc.setMaxIdleTime(100);
        int maxThreads = 100;
        int minThreads = maxThreads;
        QueuedThreadPool pool = new QueuedThreadPool(maxThreads);
        pool.setMinThreads(minThreads);
        server.setThreadPool(pool);
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            LOGGER.error("stop server failed:", e);
        }
    }

    @Override
    public void setServices(List<String> serviceList) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRegisterAddr(String serverAddr) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setReactors(int size) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setServerAddr(String ip, int port) {
        // TODO Auto-generated method stub

    }

}

package com.fangcloud.phoenix.server.connection;

import java.nio.channels.SelectionKey;

/**
 * Phoneinx-proxy与服务提供者之间的连接
 * 
 * @author chenke
 * @date 2017年3月22日 下午1:23:39
 */
public interface ProviderConnection extends NIOConnection {

    public String getAddress();

    public int getPort();

    public SelectionKey getSelectionKey();

    public void setFrontConnection(FrontConnection fconn);

}

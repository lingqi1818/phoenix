package com.fangcloud.phoenix.server;

/**
 * 服务器异常
 * 
 * @author chenke
 * @date 2017年3月21日 上午10:00:37
 */
public class ServerException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

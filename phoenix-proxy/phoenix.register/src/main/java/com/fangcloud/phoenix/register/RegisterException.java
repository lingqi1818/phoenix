package com.fangcloud.phoenix.register;

/**
 * 注册中心异常
 * 
 * @author chenke
 * @date 2017年3月21日 上午10:00:37
 */
public class RegisterException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RegisterException(String message) {
        super(message);
    }

    public RegisterException(String message, Throwable cause) {
        super(message, cause);
    }
}

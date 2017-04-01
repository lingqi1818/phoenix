package com.fangcloud.phoenix.protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 协议解析器
 * 
 * @author chenke
 * @date 2017年3月23日 下午1:21:52
 */
public interface ProtocolParser {
    /**
     * 协议头数据
     * 
     * @author chenke
     * @date 2017年3月23日 下午1:46:39
     */
    public static class Header {
        private Map<String, String> params = new HashMap<String, String>();
        private int                 bodyLength;
        private String              requestLine;
        private String              serviceName;

        public String getRequestLine() {
            return requestLine;
        }

        public void setRequestLine(String requestLine) {
            this.requestLine = requestLine;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParam(String key, String value) {
            this.params.put(key, value);
        }

        public int getBodyLength() {
            return bodyLength;
        }

        public void setBodyLength(int bodyLength) {
            this.bodyLength = bodyLength;
        }
    }

    /**
     * 进行头部解析工作
     * 
     * @param headerBuf
     * @return
     * @throws IOException
     */
    public Header parseHeader(byte[] headerBuf) throws IOException;

}

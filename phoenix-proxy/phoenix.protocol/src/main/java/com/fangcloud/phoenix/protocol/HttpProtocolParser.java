package com.fangcloud.phoenix.protocol;

import java.io.IOException;

/**
 * http协议解析器
 * 
 * @author chenke
 * @date 2017年3月23日 下午1:50:34
 */
public class HttpProtocolParser implements ProtocolParser {

    @Override
    public Header parseHeader(byte[] headerBuf) throws IOException {
        Header header = new Header();
        try {
            int index = parseRequestLine(header, headerBuf);
            parseHeaderParams(header, headerBuf, index);
        } catch (Exception ex) {
            throw new IOException("parse header error:" + ex);
        }
        return header;
    }

    private void parseHeaderParams(Header header, byte[] headerBuf, int index) {
        int flag = index;
        String key = null;
        boolean isHeadFinish = false;
        while (index < headerBuf.length && !isHeadFinish) {

            switch (headerBuf[index]) {
                case ' ':
                    if (headerBuf[index - 1] == ':') {
                        key = new String(headerBuf, flag, index - flag - 1);
                        flag = index + 1;
                    }
                    break;

                case '\n':
                    if (headerBuf[index - 1] == '\r') {
                        if (headerBuf[index + 1] == '\r' && headerBuf[index + 2] == '\n') {
                            isHeadFinish = true;
                        }

                        if (key != null) {

                            String value = new String(headerBuf, flag, index - flag - 1);
                            header.setParam(key, value);
                            if (key.equalsIgnoreCase("Content-Length")) {
                                header.setBodyLength(Integer.valueOf(value));
                            }
                            flag = index + 1;
                            key = null;
                        }
                    }
                    break;

                default:
                    break;
            }

            index++;

        }
    }

    private int parseRequestLine(Header header, byte[] headerBuf) {
        int index = 0;
        int serviceNameStart = -1, serviceNameEnd = -1;
        for (;;) {
            if (headerBuf[index] == '/' && serviceNameStart == -1) {
                serviceNameStart = index + 1;
            } else if (headerBuf[index] == ' ' && serviceNameStart > 0 && serviceNameEnd == -1) {
                serviceNameEnd = index - 1;
                header.setServiceName(new String(headerBuf, serviceNameStart,
                        serviceNameEnd - serviceNameStart + 1));
            } else if (headerBuf[index] == '\r' && headerBuf[index + 1] == '\n') {
                break;
            }
            index++;
        }

        int requestLineLength = index;
        header.setRequestLine(new String(headerBuf, 0, requestLineLength));
        return index + 2;
    }

}

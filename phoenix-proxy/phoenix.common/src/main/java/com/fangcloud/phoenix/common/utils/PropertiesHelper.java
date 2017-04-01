package com.fangcloud.phoenix.common.utils;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Properties读取帮助类
 * 
 * @author chenke
 * @date 2017年2月22日 下午1:17:38
 */
public class PropertiesHelper {
    private static final Logger LOGGER = Logger.getLogger(PropertiesHelper.class);
    private Properties          props  = new Properties();

    public PropertiesHelper(String propFile) {

        try {
            props.load(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream(propFile));
        } catch (Exception e) {
            LOGGER.error("load " + propFile + " faild !");
        }
    }

    public String getProp(String key) {
        return props.getProperty(key);
    }

    public String getProp(String key, String defaultVal) {
        String val = getProp(key);
        return StringUtils.isEmpty(val) ? defaultVal : val;
    }

    public long getLongNumberProp(String key, int defaultVal) {
        String val = getProp(key);
        return StringUtils.isEmpty(val) ? defaultVal : Long.valueOf(val);
    }

    public int getIntNumberProp(String key, int defaultVal) {
        String val = getProp(key);
        return StringUtils.isEmpty(val) ? defaultVal : Integer.valueOf(val);
    }

    public Set<Object> keySet() {
        return props.keySet();
    }
}

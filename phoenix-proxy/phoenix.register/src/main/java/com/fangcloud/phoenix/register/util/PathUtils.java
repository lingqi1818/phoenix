package com.fangcloud.phoenix.register.util;

/**
 * zk路径访问工具类
 * 
 * @author chenke
 * @date 2017年3月21日 上午10:20:55
 */
public class PathUtils {
    public static String pathOf(String parent, String children) {
        return parent + "/" + children;
    }

    public static String dubboRootPath() {
        return pathOf("", "dubbo");
    }

    public static String servicePath(String serviceName) {
        return pathOf(dubboRootPath(), serviceName);
    }

    public static String consumersPath(String serviceName) {
        return pathOf(servicePath(serviceName), "consumers");
    }

    public static String providersPath(String serviceName) {
        return pathOf(servicePath(serviceName), "providers");
    }
}

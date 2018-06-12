package com.lianxi.server;

import java.nio.charset.Charset;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 17:20 2018/6/11
 */
public class CurrentUtil {
    public static Charset UTF8 = Charset.forName("utf8");

    public static String normalize(String uri) {
        // "/" => "/"
        // "" => "/"
        // "/a/b/" => "/a/b"
        // "a/b/" => "/a/b"
        uri = uri.trim();
        // 删除后缀的/
        while (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        // 删除前缀的/
        while (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        // 前缀补充/
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri;
    }
}

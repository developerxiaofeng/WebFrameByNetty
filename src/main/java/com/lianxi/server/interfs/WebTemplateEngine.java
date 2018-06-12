package com.lianxi.server.interfs;

import com.lianxi.server.AbortException;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 17:00 2018/6/11
 */

public interface WebTemplateEngine {
    public default String render(String path, Map<String, Object> context) {
        throw new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "template root not provided");
    }
}

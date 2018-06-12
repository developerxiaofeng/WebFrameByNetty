package com.lianxi.server.interfs;

import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 16:59 2018/6/11
 */
@FunctionalInterface
public interface WebRequestHandler {
    public void handle(ApplicationContext ctx, Request req);
}

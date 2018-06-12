package com.lianxi.server.interfs;

import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 16:58 2018/6/11
 */
@FunctionalInterface
public interface WebRequestFilter {
    public boolean filter(ApplicationContext ctx, Request req, boolean beforeOrAfter);
}

package com.lianxi.server.interfs;

import com.lianxi.server.AbortException;
import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 16:56 2018/6/11
 */
@FunctionalInterface
public interface WebExceptionHandler {
    public void handle(ApplicationContext ctx, AbortException e);
}

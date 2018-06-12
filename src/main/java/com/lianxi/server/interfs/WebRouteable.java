package com.lianxi.server.interfs;

import com.lianxi.server.Router;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 17:00 2018/6/11
 */
@FunctionalInterface
public interface WebRouteable {
    public Router route();
}

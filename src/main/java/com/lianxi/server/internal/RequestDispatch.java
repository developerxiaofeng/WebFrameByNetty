package com.lianxi.server.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 14:51 2018/6/11
 */
public interface RequestDispatch {
    public void dispatch(ChannelHandlerContext ctx, FullHttpRequest req);
}

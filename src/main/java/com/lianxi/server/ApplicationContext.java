package com.lianxi.server;

import com.alibaba.fastjson.JSON;
import com.lianxi.server.interfs.WebTemplateEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 17:11 2018/6/11
 */
public class ApplicationContext {
    private ChannelHandlerContext ctx;
    private WebTemplateEngine templateEngine;
    private  String contextRoot;

    private Set<Cookie> cookies=new HashSet<>();

    public ApplicationContext(ChannelHandlerContext ctx,
                              String contextRoot,WebTemplateEngine templateEngine){
        this.ctx=ctx;
        this.contextRoot=contextRoot;
        this.templateEngine=templateEngine;
    }

    public void  send(Object ...response){
        for (Object o:response){
            ctx.write(o);
        }
        ctx.flush();
        cookies.clear();
    }

    public String contextRoot() {
        return contextRoot;
    }

    public ApplicationContext addCookie(String name, String value) {
        return this.addCookie(name, value, null, this.contextRoot, -1, false, false);
    }

    public ApplicationContext addCookie(String name, String value, String domain, String path, long maxAge, boolean httpOnly,
                                 boolean isSecure) {
        Cookie cookie = null;
        for (Cookie ck : cookies) {
            if (ck.name().equals(name)) {
                cookie = ck;
                break;
            }
        }
        if (cookie == null) {
            cookie = new DefaultCookie(name, value);
            if (domain != null){
                cookie.setDomain(domain);}
            if (path != null){
                cookie.setPath(path);}
            if (cookie.maxAge() >= 0){
                cookie.setMaxAge(maxAge);}
            cookie.setHttpOnly(httpOnly);
            cookie.setSecure(isSecure);
            cookies.add(cookie);
        }
        return this;
    }

    public ApplicationContext removeCookie(String name) {
        return addCookie(name, "", null, this.contextRoot, 0, false, false);
    }

    public ByteBufAllocator alloc() {
        return ctx.alloc();
    }

    public void redirect(String location) {
        redirect(location, true);
    }

    public void redirect(String location, boolean withinContext) {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        if (location.startsWith("/")) {
            location = withinContext ? Paths.get(contextRoot, location).toString() : location;
        }
        res.headers().add(HttpHeaderNames.LOCATION, location);

        for (Cookie cookie : cookies) {
            res.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
        }
        cookies.clear();
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }

    public void text(String content, String contentType, int statusCode) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        byte[] bytes = content.getBytes(CurrentUtil.UTF8);
        buf.writeBytes(bytes);
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(statusCode), buf);
        res.headers().add(HttpHeaderNames.CONTENT_TYPE, String.format("%s; charset=utf-8", contentType));
        res.headers().add(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        for (Cookie cookie : cookies) {
            res.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
        }
        ctx.writeAndFlush(res);
        cookies.clear();
    }

    public void abort(int code, String content) {
        throw new AbortException(HttpResponseStatus.valueOf(code), content);
    }

    public void html(String html) {
        html(html, 200);
    }

    public void html(String html, int statusCode) {
        text(html, "text/html", statusCode);
    }

    public void error(int statusCode) {
        error(HttpResponseStatus.valueOf(statusCode).reasonPhrase(), statusCode);
    }

    public void error(String msg, int statusCode) {
        error(msg, "text/plain", statusCode);
    }

    public void error(String msg, String contentType, int statusCode) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        byte[] bytes = msg.getBytes(CurrentUtil.UTF8);
        buf.writeBytes(bytes);
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(statusCode), buf);
        res.headers().add(HttpHeaderNames.CONTENT_TYPE, String.format("%s; charset=utf-8", contentType));
        for (Cookie cookie : cookies) {
            res.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
        }
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        cookies.clear();
    }

    public void render(String path) {
        render(path, 200);
    }

    public void render(String path, Map<String, Object> context) {
        render(path, context, 200);
    }

    public void render(String path, int statusCode) {
        render(path, Collections.emptyMap(), statusCode);
    }

    public void render(String path, Map<String, Object> context, int statusCode) {
        html(templateEngine.render(path, context), statusCode);
    }

    public void json(Object o, int statusCode) {
        text(JSON.toJSONString(o), "application/json", statusCode);
    }

    public void json(Object o) {
        json(o, 200);
    }

    public void close() {
        this.ctx.close();
    }

}

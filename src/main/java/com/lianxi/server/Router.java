package com.lianxi.server;

import com.lianxi.server.interfs.WebRequestFilter;
import com.lianxi.server.interfs.WebRequestHandler;
import com.lianxi.server.interfs.WebRouteable;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.*;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 16:53 2018/6/11
 */
//用于构建路由，它负责的是将URL规则和RequestHandler挂接起来，形成一个复杂的映射表。
//为了简化实现细节，所以没有支持复杂的URL规则，
// 例如像RESTFUL这种将参数写在URL里面的这种形式是不支持的。
public class Router {
    private WebRequestHandler wirecardHandler;

    private Map<String, WebRequestHandler> subHandlers = new HashMap<>();
    private Map<String, Map<String, WebRequestHandler>> subMethodHandlers = new HashMap<>();

    private Map<String, Router> subRouters = new HashMap<>();

    private List<WebRequestFilter> filters = new ArrayList<>();

    private final static List<String> METHODS = Arrays
            .asList(new String[] { "get", "post", "head", "put", "delete", "trace", "options", "patch", "connect" });

    public Router() {
        this(null);
    }

    public Router(WebRequestHandler wirecardHandler) {
        this.wirecardHandler = wirecardHandler;
    }

    public Router handler(String path, WebRequestHandler handler) {
        path = CurrentUtil.normalize(path);
        if (path.indexOf('/') != path.lastIndexOf('/')) {
            throw new IllegalArgumentException("path at most one slash allowed");
        }
        this.subHandlers.put(path, handler);
        return this;
    }

    public Router handler(String path, String method, WebRequestHandler handler) {
        path = CurrentUtil.normalize(path);
        method = method.toLowerCase();
        if (path.indexOf('/') != path.lastIndexOf('/')) {
            throw new IllegalArgumentException("path at most one slash allowed");
        }
        if (!METHODS.contains(method)) {
            throw new IllegalArgumentException("illegal http method name");
        }
        Map<String, WebRequestHandler> handlers = subMethodHandlers.get(path);
        if (handlers == null) {
            handlers = new HashMap<>();
            subMethodHandlers.put(path, handlers);
        }
        handlers.put(method, handler);
        return this;
    }

    public Router child(String path, Router router) {
        path = CurrentUtil.normalize(path);
        if (path.equals("/")) {
            throw new IllegalArgumentException("child path should not be /");
        }
        if (path.indexOf('/') != path.lastIndexOf('/')) {
            throw new IllegalArgumentException("path at most one slash allowed");
        }
        this.subRouters.put(path, router);
        return this;
    }

    public Router child(String path, WebRouteable routeable) {
        return child(path, routeable.route());
    }

    public Router resource(String path, String resourceRoot) {
        Router router = new Router(new StaticRequestHandler(resourceRoot));
        return child(path, router);
    }

    public Router resource(String path, String resourceRoot, boolean classpath) {
        Router router = new Router(new StaticRequestHandler(resourceRoot, classpath));
        return child(path, router);
    }

    public Router resource(String path, String resourceRoot, boolean classpath, int cacheAge) {
        Router router = new Router(new StaticRequestHandler(resourceRoot, classpath, cacheAge));
        return child(path, router);
    }

    public Router filter(WebRequestFilter... filters) {
        for (WebRequestFilter filter : filters) {
            this.filters.add(filter);
        }
        return this;
    }

    public void handle(ApplicationContext ctx, Request req) {
        for (WebRequestFilter filter : filters) {
            req.filter(filter);
        }
        String prefix = req.peekUriPrefix();
        String method = req.method().toLowerCase();
        Router router = subRouters.get(prefix);
        if (router != null) {
            req.popUriPrefix();
            router.handle(ctx, req);
            return;
        }

        if (prefix.equals(req.relativeUri())) {
            Map<String, WebRequestHandler> handlers = subMethodHandlers.get(prefix);
            WebRequestHandler handler = null;
            if (handlers != null) {
                handler = handlers.get(method);
            }
            if (handler == null) {
                handler = subHandlers.get(prefix);
            }
            if (handler != null) {
                handleImpl(handler, ctx, req);
                return;
            }
        }

        if (this.wirecardHandler != null) {
            handleImpl(wirecardHandler, ctx, req);
            return;
        }

        throw new AbortException(HttpResponseStatus.NOT_FOUND);
    }

    private void handleImpl(WebRequestHandler handler, ApplicationContext ctx, Request req) {
        for (WebRequestFilter filter : req.filters()) {
            if (!filter.filter(ctx, req, true)) {
                return;
            }
        }

        handler.handle(ctx, req);

        for (WebRequestFilter filter : req.filters()) {
            if (!filter.filter(ctx, req, false)) {
                return;
            }
        }
    }
}

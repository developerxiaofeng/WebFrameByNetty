package com.lianxi.server;


import com.lianxi.server.interfs.WebExceptionHandler;
import com.lianxi.server.interfs.WebTemplateEngine;
import com.lianxi.server.internal.RequestDispatch;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 17:26 2018/6/11
 */
//是请求派发器，用于将收到的HTTP请求对象扔给响应的RequestHandler进行处理。
public class RequestDispatcherImpl implements RequestDispatch {
    private final static Logger LOG = LoggerFactory.getLogger(RequestDispatcherImpl.class);

    private String contextRoot;
    private Router router;
    private Map<Integer, WebExceptionHandler> exceptionHandlers = new HashMap<>();
    private WebExceptionHandler defaultExceptionHandler = new DefaultExceptionHandler();

    private WebTemplateEngine templateEngine = new WebTemplateEngine() {
    };

    static class DefaultExceptionHandler implements WebExceptionHandler {

        @Override
        public void handle(ApplicationContext ctx, AbortException e) {
            if (e.getStatus().code() == 500) {
                LOG.error("Internal Server Error", e);
            }
            ctx.error(e.getContent(), e.getStatus().code());
        }

    }

    public RequestDispatcherImpl(Router router) {
        this("/", router);
    }

    public RequestDispatcherImpl(String contextRoot, Router router) {
        this.contextRoot = CurrentUtil.normalize(contextRoot);
        this.router = router;
    }

    public RequestDispatcherImpl templateRoot(String templateRoot) {
        this.templateEngine = new FreemarkerEngine(templateRoot);
        return this;
    }

    public String root() {
        return contextRoot;
    }

    public RequestDispatcherImpl exception(int code, WebExceptionHandler handler) {
        this.exceptionHandlers.put(code, handler);
        return this;
    }

    public RequestDispatcherImpl exception(WebExceptionHandler handler) {
        this.defaultExceptionHandler = handler;
        return this;
    }
    @Override
    public void dispatch(ChannelHandlerContext channelCtx, FullHttpRequest req) {
        ApplicationContext ctx = new ApplicationContext(channelCtx, contextRoot, templateEngine);
        try {
            this.handleImpl(ctx, new Request(req));
        } catch (AbortException e) {
            this.handleException(ctx, e);
        } catch (Exception e) {
            this.handleException(ctx, new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e));
        } finally {
            req.release();
        }
    }

    private void handleException(ApplicationContext ctx, AbortException e) {
        WebExceptionHandler handler = this.exceptionHandlers.getOrDefault(e.getStatus().code(), defaultExceptionHandler);
        try {
            handler.handle(ctx, e);
        } catch (Exception ex) {
            this.defaultExceptionHandler.handle(ctx, new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR, ex));
        }
    }

    private void handleImpl(ApplicationContext ctx, Request req) throws Exception {
        if (req.decoderResult().isFailure()) {
            ctx.abort(400, "http protocol decode failed");
        }
        if (req.relativeUri().contains("./") || req.relativeUri().contains(".\\")) {
            ctx.abort(400, "unsecure url not allowed");
        }
        if (!req.relativeUri().startsWith(contextRoot)) {
            throw new AbortException(HttpResponseStatus.NOT_FOUND);
        }
        req.popRootUri(contextRoot);
        router.handle(ctx, req);
    }
}

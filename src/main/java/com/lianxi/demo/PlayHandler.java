package com.lianxi.demo;

import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;
import com.lianxi.server.Router;
import com.lianxi.server.interfs.WebRouteable;


import java.util.HashMap;
import java.util.Map;

public class PlayHandler implements WebRouteable {

	public void play(ApplicationContext ctx, Request req) {
		Map params = new HashMap<String, Object>();
		params.put("req", req);
		ctx.render("playground.ftl", params);
	}

	@Override
	public Router route() {
		Router router = new Router();
		router.handler("/", this::play);
		return router;
	}

}

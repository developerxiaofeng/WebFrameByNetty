package com.lianxi.demo;

import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;
import com.lianxi.server.Router;
import com.lianxi.server.interfs.WebRouteable;


import java.util.HashMap;
import java.util.Map;

public class UserHandler implements WebRouteable {

	public void home(ApplicationContext ctx, Request req) {
		User user = req.attr("user");
		user.incrCounter();
		Map params = new HashMap<String, Object>();
		params.put("user", req.attr("user"));
		ctx.render("home.ftl", params);
	}

	public void getCounter(ApplicationContext ctx, Request req) {
		Map res = new HashMap<String, Object>();
		User user = req.attr("user");
		res.put("counter", user.getCounter());
		ctx.json(res);
	}

	@Override
	public Router route() {
		Router router = new Router();
		router.handler("/", "GET", this::home);
		router.handler("/counter.json", "GET", this::getCounter);
		return router;
	}

}

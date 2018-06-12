package com.lianxi.demo;

import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;
import com.lianxi.server.Router;
import com.lianxi.server.interfs.WebRouteable;


import java.util.UUID;

public class SecurityHandler implements WebRouteable {

	private UserDB db;
	private MemorySession session;

	public SecurityHandler(UserDB db, MemorySession session) {
		this.db = db;
		this.session = session;
	}

	public void getLoginPage(ApplicationContext ctx, Request req) {
		ctx.render("login.ftl");
	}

	public void login(ApplicationContext ctx, Request req) {
		String name = req.mixedParam("name");
		String passwd = req.mixedParam("passwd");
		if (!db.checkAccess(name, passwd)) {
			ctx.abort(401, "用户名密码错误");
		}
		User user = new User(name);
		String sid = UUID.randomUUID().toString();
		session.setUser(sid, user);
		ctx.addCookie("kids_sid", sid);
		ctx.redirect("/user");
	}

	@Override
	public Router route() {
		Router router = new Router();
		router.handler("/login", "GET", this::getLoginPage);
		router.handler("/login", "POST", this::login);
		return router;
	}

}

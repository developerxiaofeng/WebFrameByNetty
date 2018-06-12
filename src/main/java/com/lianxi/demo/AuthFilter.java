package com.lianxi.demo;

import com.lianxi.server.ApplicationContext;
import com.lianxi.server.Request;
import com.lianxi.server.interfs.WebRequestFilter;

public class AuthFilter implements WebRequestFilter {

	private MemorySession session;

	public AuthFilter(MemorySession session) {
		this.session = session;
	}

	@Override
	public boolean filter(ApplicationContext ctx, Request req, boolean beforeOrAfter) {
		if (!beforeOrAfter) {
			return true;
		}
		String sid = req.cookie("kids_sid");
		if (sid != null) {
			User user = session.getUser(sid);
			if (user != null) {
				req.attr("user", user);
				return true;
			}
		}

		ctx.redirect("/security/login");
		return false;
	}

}

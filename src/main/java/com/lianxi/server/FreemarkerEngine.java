package com.lianxi.server;


import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import com.lianxi.server.interfs.WebTemplateEngine;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.netty.handler.codec.http.HttpResponseStatus;
/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 17:01 2018/6/11
 */
//freemarker专门为MVC设计,专注视图展示,不依赖servlet
public class FreemarkerEngine implements WebTemplateEngine {

    private Configuration config;

    public FreemarkerEngine(String templateRoot) {
        //初始化模板配置
        this.config = new Configuration(Configuration.VERSION_2_3_28);
        this.config.setClassForTemplateLoading(FreemarkerEngine.class, templateRoot);
    }

    @Override
    public String render(String path, Map<String, Object> context) {
        try {
            //获得模板
            Template template = config.getTemplate(path, "utf-8");
            StringWriter writer = new StringWriter();
            //生成文件（这里是我们是生成html）
            template.process(context, writer);
            return writer.toString();
        } catch (IOException e) {
           throw new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } catch (TemplateException e) {
            throw new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

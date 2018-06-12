package com.lianxi.server.internal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 14:41 2018/6/11
 */
public class HttpServer {
    private final static Logger LOG= LoggerFactory.getLogger(HttpServer.class);
    private String ip;
    private int port;  //端口
    private int ioThreads;  //IO线程数,用于处理套接字读写,由Netty内部管理
    private int workerThreads;  //业务线程数,专门处理http请求,由我们本省框架管理
    private RequestDispatch requestDispatch;//请求配发器对象

    public HttpServer() {
    }

    public HttpServer(String ip, int port, int ioThreads,
                      int workerThreads, RequestDispatch requestDispatch) {
        this.ip = ip;
        this.port = port;
        this.ioThreads = ioThreads;
        this.workerThreads = workerThreads;
        this.requestDispatch = requestDispatch;
    }
    //用于服务端,使用一个ServerChannel接收客户端的连接,
    // 并创建对应的子Channel
    private ServerBootstrap bootstrap;
    //包含多个EventLoop
    private EventLoopGroup group;
    //代表一个Socket连接
    private Channel serverChannel;
    //
    private MessageCollector collector;

    public  void start(){
        bootstrap=new ServerBootstrap();
        group=new NioEventLoopGroup(ioThreads);
        bootstrap.group(group);
        collector=new MessageCollector(workerThreads,requestDispatch);
        bootstrap.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline=socketChannel.pipeline();
                //如果客户端60秒没有任何请求,就关闭客户端连接
                pipeline.addLast(new ReadTimeoutHandler(10));
                //客户端和服务器简单的编解码器：HttpClientCodec和HttpServerCodec。
                //ChannelPipelien中有解码器和编码器(或编解码器)后就可以操作不同的HttpObject消息了；但是HTTP请求和响应可以有很多消息数据，
                // 你需要处理不同的部分，可能也需要聚合这些消息数据
                pipeline.addLast(new HttpServerCodec());
                //通过HttpObjectAggregator，Netty可以聚合HTTP消息，
                // 使用FullHttpResponse和FullHttpRequest到ChannelPipeline中的下一个ChannelHandler，这就消除了断裂消息，保证了消息的完整
                pipeline.addLast(new HttpObjectAggregator(1 << 30)); // max_size = 1g
                //允许通过处理ChunkedInput来写大的数据块
                pipeline.addLast(new ChunkedWriteHandler());
                //将业务处理器放到最后
                pipeline.addLast(collector);
            }
        });

        bootstrap.option(ChannelOption.SO_BACKLOG, 100).option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true);
        serverChannel = bootstrap.bind(this.ip, this.port).channel();
        LOG.warn("server started @ {}:{}", ip, port);
    }

    public void stop() {
        // 先关闭服务端套件字
        serverChannel.close();
        // 再斩断消息来源，停止io线程池
        group.shutdownGracefully();
        // 最后停止业务线程
        collector.closeGracefully();
    }

}

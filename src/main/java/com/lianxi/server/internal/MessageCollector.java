package com.lianxi.server.internal;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 15:00 2018/6/11
 */
//Netty 事件回调类
@Sharable
public class MessageCollector extends ChannelInboundHandlerAdapter {
    private final static Logger LOG = LoggerFactory.getLogger(MessageCollector.class);
    //业务线程池
    private ThreadPoolExecutor[] executors;
    private RequestDispatch requestDispatch;
    //业务队列最大值
    private int requestsMaxInflight=1000;

    public MessageCollector(int workerThreads,RequestDispatch dispatch){
        //给业务线程命名
        ThreadFactory factory =new ThreadFactory() {
            AtomicInteger seq=new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread =new Thread(r);
                thread.setName("http-"+seq.getAndIncrement());
                return thread;
            }
        };
        this.executors=new ThreadPoolExecutor[workerThreads];
        for(int i=0;i<workerThreads;i++){
            ArrayBlockingQueue queue=new ArrayBlockingQueue<Runnable>(requestsMaxInflight);
            ////闲置时间超过30秒的线程就自动销毁
            this.executors[i]=new ThreadPoolExecutor(1,1,
                    30, TimeUnit.SECONDS, queue,factory,new CallerRunsPolicy());
        }

        this.requestDispatch=dispatch;
    }

    public  void  closeGracefully(){
        //优雅一点关闭,先通知,再等待,最后强制关闭
        for (int i=0;i<executors.length;i++){
            ThreadPoolExecutor executor=executors[i];
            try {
                executor.awaitTermination(10,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executor.shutdownNow();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //客户端来了一个新的连接
       LOG.info("connection comes {}",ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //客户端走了一个
        LOG.info("connection leaves {}",ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest){
            FullHttpRequest req= (FullHttpRequest) msg;
            CRC32 crc32=new CRC32();
            crc32.update(ctx.hashCode());
            int idx =(int) (crc32.getValue()%executors.length);
            //用业务线程处理消息
            this.executors[idx].execute(() ->{
                requestDispatch.dispatch(ctx,req);
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //此处可能因为客户机器突发重启
        //也可能客户端连接时间超时,后面的REadTimeoutHandle抛出异常
        //也可能消息协议错误,序列化异常
        ctx.close();
    }
}

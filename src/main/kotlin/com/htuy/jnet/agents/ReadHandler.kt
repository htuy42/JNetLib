package com.htuy.jnet.agents

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.util.ReferenceCountUtil

class ReadHandler(val onRead : (ChannelHandlerContext,Any) -> Boolean) : ChannelInboundHandlerAdapter(){
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if(onRead(ctx,msg)){
            ReferenceCountUtil.release(msg)
        }
        else{
            super.channelRead(ctx, msg)
        }
    }
}

class ActiveHandler(val onActive : (ChannelHandlerContext) -> Unit) : ChannelInboundHandlerAdapter(){
    override fun channelActive(ctx: ChannelHandlerContext) {
        onActive(ctx)
    }
}

class InactiveHander(val onInactive : (ChannelHandlerContext) -> Unit) : ChannelInboundHandlerAdapter(){
    override fun channelInactive(ctx: ChannelHandlerContext) {
        onInactive(ctx)
    }
}

class WriteHandler(val onRead : (ChannelHandlerContext,Any,ChannelPromise) -> Boolean) : ChannelOutboundHandlerAdapter(){
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if(!onRead(ctx,msg,promise)){
            super.write(ctx, msg, promise)
        }
    }
}
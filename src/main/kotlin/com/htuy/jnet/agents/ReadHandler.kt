package com.htuy.jnet.agents

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
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
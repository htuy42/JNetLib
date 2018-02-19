package com.htuy.jnet.agents

import com.htuy.jnet.messages.LifecycleHandler
import com.htuy.jnet.messages.MessageHandler
import com.htuy.kt.stuff.LOGGER
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil

class ConnectionManager(private val handles: List<MessageHandler>,
                        private val initFunction: LifecycleHandler,
                        private val cleanupFunction: LifecycleHandler) : ChannelInboundHandlerAdapter() {


    override fun channelInactive(ctx: ChannelHandlerContext) {
        LOGGER.debug { "Channel closed down" }
        cleanupFunction.handle(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext,
                             msg: Any?) {
        for (mh in handles) {
            LOGGER.trace { "Read message from channel : ${msg.toString()}" }
            if (mh.handles(msg)) {
                mh.handle(ctx, msg)
                if (mh.cleanupRequired) {
                    ReferenceCountUtil.release(msg)
                }
                break
            }
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        LOGGER.debug("Channel initialized")
        initFunction.handle(ctx)
    }
}
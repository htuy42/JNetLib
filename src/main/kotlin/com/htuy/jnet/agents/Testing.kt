package com.htuy.jnet.agents

import com.htuy.jnet.messages.MessageHandler
import io.netty.channel.ChannelHandlerContext


class EchoHandler : MessageHandler {
    override fun handles(msg: Any?): Boolean {
        return true
    }

    override fun handle(ctx: ChannelHandlerContext, msg: Any?) {
        ctx.writeAndFlush(msg)
    }

    override var cleanupRequired: Boolean = false
}
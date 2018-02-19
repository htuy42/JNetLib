package com.htuy.jnet.messages

import com.htuy.kt.stuff.FunctionFactory
import io.netty.channel.ChannelHandlerContext


interface MessageHandler {
    fun handles(msg: Any?): Boolean

    fun handle(ctx: ChannelHandlerContext,
               msg: Any?)

    var cleanupRequired: Boolean
}

class MessageTypeFunHandler<T>(private val type: Class<out T>,
                               private val handler: (ChannelHandlerContext, T) -> Unit) : MessageHandler {
    override var cleanupRequired = true

    override fun handles(msg: Any?): Boolean {
        return msg != null && type.isInstance(msg)
    }

    override fun handle(ctx: ChannelHandlerContext,
                        msg: Any?) {
        handler(ctx, type.cast(msg))
    }


}

interface LifecycleHandler {
    fun handle(ctx: ChannelHandlerContext)
}

class LambdaLifecycleHandler(val lam: (ChannelHandlerContext) -> Unit) : LifecycleHandler {
    override fun handle(ctx: ChannelHandlerContext) {
        lam(ctx)
    }
}

class NullHandler : LifecycleHandler {
    override fun handle(ctx: ChannelHandlerContext) {}
}


val NullHandlerFactory = FunctionFactory({ NullHandler() })
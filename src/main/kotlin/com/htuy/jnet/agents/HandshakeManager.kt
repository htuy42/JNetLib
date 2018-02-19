package com.htuy.jnet.agents

import com.htuy.jnet.messages.HandshakeMessage
import com.htuy.kt.stuff.LOGGER
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import mu.KLogging


//note, its not *really* managing passwords in any meaningful sense, the security it provides is basically 0.
// Its mostly just to make sure you don't accidentally connect to something you aren't expecting to.
class HandshakeManager(val replacement: ChannelInboundHandlerAdapter,
                       val isServer: Boolean,
                       val password: String)
    : ChannelInboundHandlerAdapter() {
    companion object : KLogging()

    override fun channelRead(ctx: ChannelHandlerContext,
                             msg: Any) {
        if (isServer) {
            if (msg is HandshakeMessage) {
                if (msg.password == password) {
                    LOGGER.debug { "Successful password match detected, adding new element into pipeline and sending confirmation" }
                    ctx.channel()
                            .writeAndFlush(msg.confirm())
                    ctx.pipeline()
                            .replace(this, "handler", replacement)
                    replacement.channelActive(ctx)
                } else {
                    LOGGER.error { "Invalid pw expressed by remote client." }
                    LOGGER.error { "Our pw was " + password + " and theirs was " + msg.password }
                    ctx.channel()
                            .close()
                }
            } else {
                throw IllegalStateException("Handshake manager not paired with a handshake manager on the other side.")
            }
        } else {
            msg as HandshakeMessage
            if (msg.confirmed) {
                LOGGER.debug { "Received confirmation message. Replacing into pipeline." }
                ctx.pipeline()
                        .replace(this, "handler", replacement)
            } else {
                LOGGER.error { "Received unexpected handshake message that is not confirmed from remote host." }
            }
        }
        ReferenceCountUtil.release(msg)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        if (!isServer) {
            LOGGER.debug { "Channel active, sending handshake message to server with password $password" }
            ctx.channel()
                    .writeAndFlush(HandshakeMessage(password))
        }
        super.channelActive(ctx)
    }
}


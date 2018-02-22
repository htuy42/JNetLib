package com.htuy.jnet.agents

import com.htuy.jnet.messages.Message
import com.htuy.jnet.messages.MessageHandler
import com.htuy.jnet.protocol.Protocol
import com.htuy.jnet.protocol.ProtocolBuilder
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import java.util.*

class Client(val hostAddr: String,
             val port: Int,
             val protocol: Protocol = ProtocolBuilder(),
             val lastHandler: ChannelHandler? = null,
             val lastHandlerList : List<MessageHandler>? = null) {

    var channel: Channel? = null
    private val workerGroup = NioEventLoopGroup()
    private val installAfterPairs: MutableCollection<Triple<String, String, ChannelHandler>> = Collections.synchronizedList(
            ArrayList())
    private val installBeforePairs: MutableCollection<Triple<String, String, ChannelHandler>> = Collections.synchronizedList(
            ArrayList())

    fun connect(): ChannelFuture {
        val b = Bootstrap()
        b.group(workerGroup)
        b.channel(NioSocketChannel::class.java)
        b.option(ChannelOption.SO_KEEPALIVE, true)
        b.handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline().addLast("decoder",ObjectDecoder(ClassResolvers.weakCachingResolver(null)))
                        .addLast("encoder",ObjectEncoder())
                for (elt in installAfterPairs) {
                    ch.pipeline().addAfter(elt.first, elt.second, elt.third)
                }
                for(elt in installBeforePairs) {
                    ch.pipeline().addBefore(elt.first,elt.second,elt.third)
                }
                if (lastHandler != null) {
                    ch.pipeline().addLast(lastHandler)
                } else if(lastHandlerList != null){
                    ch.pipeline().addLast(ConnectionManager(lastHandlerList))
                }
            }

        })
        LOGGER.debug { "Attempting connect to $hostAddr $port" }
        protocol.installToClient(b, this)
        val res = b.connect(hostAddr, port).sync()
        if (res.isSuccess) {
            LOGGER.debug { "Connect succeeded" }
        } else {
            LOGGER.error("Connect failed with some error. Channel failure : $res")
        }
        channel = res.channel()
        protocol.clientActive(res.channel(), this)
        return res
    }

    fun installAfter(after: String, installAs: String, toInstall: ChannelHandler) {
        installAfterPairs.add(Triple(after, installAs, toInstall))
    }

    fun installBefore(before : String, installAs : String, toInstall : ChannelHandler){
        installBeforePairs.add(Triple(before, installAs, toInstall))
    }

    fun sendMessage(message: Message) {
        LOGGER.trace { "Sending message $message" }
        val send = channel?.pipeline()?.writeAndFlush(message)
                ?: throw IllegalStateException("Channel not yet ready. Connect, if you are not doing so.")

        if (!send.sync().isSuccess) {
            LOGGER.trace { "Message send failed" }
        } else {
            LOGGER.trace { "Message send success" }
        }
    }

    fun shutdown() {
        val chan = channel
        if (chan != null) {
            protocol.clientDeath(chan, this)
        }
        workerGroup.shutdownGracefully()
        channel?.close()
    }
}


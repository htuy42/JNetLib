package com.htuy.jnet.agents

import com.htuy.jnet.messages.LifecycleHandler
import com.htuy.jnet.messages.Message
import com.htuy.jnet.messages.MessageHandler
import com.htuy.jnet.messages.NullHandler
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder

class Client(val hostAddr: String,
             val port: Int,
             val messageHandles: List<MessageHandler>,
             val initFunction: LifecycleHandler = NullHandler(),
             val cleanupFunction: LifecycleHandler = NullHandler(),
             val password: String = "ADMINPASS123") {

    var channel: Channel? = null

    fun connect(): ChannelFuture {
        val workerGroup = NioEventLoopGroup()
        val b = Bootstrap()
        b.group(workerGroup)
        b.channel(NioSocketChannel::class.java)
        b.option(ChannelOption.SO_KEEPALIVE, true)
        b.handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline()
                        .addLast(ObjectDecoder(ClassResolvers.weakCachingResolver(null)),
                                 ObjectEncoder(),
                                 HandshakeManager(ConnectionManager(messageHandles, initFunction, cleanupFunction),
                                                  false,
                                                  password))
            }
        })
        LOGGER.debug { "Attempting connect to $hostAddr $port" }
        val res = b.connect(hostAddr, port)
                .sync()
        if (res.isSuccess) {
            LOGGER.debug { "Connect succeeded" }
        } else {
            LOGGER.error("Connect failed ith some error. Channel failure : $res")
        }
        channel = res.channel()
        return res
    }

    fun sendMessage(message: Message) {
        LOGGER.trace { "Sending message $message" }
        val send = channel?.pipeline()?.writeAndFlush(message)
                ?: throw IllegalStateException("Channel not yet ready. Sync if you are not doing so.")

        if (!send.sync().isSuccess) {
            LOGGER.trace { "Message send failed" }
        } else {
            LOGGER.trace { "Message send success" }
        }
    }
}


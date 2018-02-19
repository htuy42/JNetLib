package com.htuy.jnet.agents

import com.htuy.jnet.messages.LifecycleHandler
import com.htuy.jnet.messages.MessageHandler
import com.htuy.jnet.messages.NullHandlerFactory
import com.htuy.kt.stuff.Factory
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder

class Server(val port: Int,
             val messageHandles: Factory<List<MessageHandler>>,
             val initFunction: Factory<LifecycleHandler> = NullHandlerFactory,
             val cleanupFunction: Factory<LifecycleHandler> = NullHandlerFactory,
             val password: String = "ADMINPASS123") {
    var channel : Channel? = null
    fun connect(): ChannelFuture {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline()
                                .addLast(ObjectEncoder(),
                                         ObjectDecoder(ClassResolvers.weakCachingResolver(null)),
                                         HandshakeManager(
                                                 ConnectionManager(
                                                         messageHandles.getInstance(),
                                                         initFunction.getInstance(),
                                                         cleanupFunction.getInstance()),
                                                 true,
                                                 password))
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
        LOGGER.debug { "Attempting server bind on port $port" }
        val res = b.bind(port)
                .sync() // (7)
        return if (res.isSuccess) {
            LOGGER.debug { "Connect successful" }
            channel = res.channel()
            res
        } else {
            LOGGER.error { "Issue connecting" }
            res
        }
    }

    fun shutdown() {
        channel?.close() ?: throw IllegalStateException("Trying to shutdown before open finished conclusively, or to shutdown " +
                                                                "an already closed server")
        channel = null
    }
}
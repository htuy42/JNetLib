package com.htuy.jnet.agents

import com.htuy.jnet.messages.MessageHandler
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import java.util.*
import kotlin.collections.ArrayList

class Server(val port: Int,
             val messageHandlesMaker: () -> List<MessageHandler>,
             val initFunctionMaker: () -> (ChannelHandlerContext) -> Unit = {{}},
             val cleanupFunctionMaker: () -> (ChannelHandlerContext) -> Unit = {{}},
             val password: String = "ADMINPASS123") {
    var channel: Channel? = null
    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()
    val installAfterPairs : MutableCollection<Triple<String,String,() -> ChannelHandler>> = Collections.synchronizedList(ArrayList())

    fun connect(): ChannelFuture {

        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("encoder", ObjectEncoder())
                                .addLast("decoder", ObjectDecoder(ClassResolvers.weakCachingResolver(null)))
                                .addAfter("decoder", "main",
                                          HandshakeManager(
                                                  ConnectionManager(
                                                          messageHandlesMaker(),
                                                          initFunctionMaker(),
                                                          cleanupFunctionMaker()),
                                                  true,
                                                  password))
                        for(elt in installAfterPairs){
                            ch.pipeline().addAfter(elt.first,elt.second,elt.third())
                        }
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

    fun installAfter(installAfter : String, installAs : String, toInstall: () -> ChannelHandler) {
        installAfterPairs.add(Triple(installAfter,installAs,toInstall))
    }

    fun shutdown() {
        LOGGER.warn{"Server shutting down."}
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
        channel?.close()
    }
}
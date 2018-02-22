package com.htuy.jnet.agents

import com.htuy.jnet.messages.Message
import com.htuy.jnet.messages.MessageHandler
import com.htuy.jnet.protocol.Protocol
import com.htuy.jnet.protocol.ProtocolBuilder
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
             val protocol : Protocol = ProtocolBuilder(),
             val lastHandler : (() -> ChannelHandler)? = null,
             val lastHandlerList: (() -> List<MessageHandler>)? = null) {
    var channel: Channel? = null
    private val bossGroup = NioEventLoopGroup()
    private val workerGroup = NioEventLoopGroup()
    val installAfterPairs : MutableCollection<Triple<String,String,() -> ChannelHandler>> = Collections.synchronizedList(ArrayList())

    fun connect(): ChannelFuture {

        val b = ServerBootstrap()
        val s = this
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("encoder", ObjectEncoder())
                                .addLast("decoder", ObjectDecoder(ClassResolvers.weakCachingResolver(null)))
                        for(elt in installAfterPairs){
                            ch.pipeline().addAfter(elt.first,elt.second,elt.third())
                        }
                        if(lastHandler!=null){
                            ch.pipeline().addLast(lastHandler?.invoke())
                        } else if(lastHandlerList != null){
                            ch.pipeline().addLast(ConnectionManager(lastHandlerList?.invoke()))
                        }
                        protocol.serverClientActive(ch,s)
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
        LOGGER.debug { "Attempting server bind on port $port" }
        protocol.installToServer(b,this)
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

    fun sendMessage(chan : Channel, toSend: Message) {
        chan.writeAndFlush(toSend) ?: throw IllegalStateException("Tried to send a message before the server was properly initialized.")
    }
}
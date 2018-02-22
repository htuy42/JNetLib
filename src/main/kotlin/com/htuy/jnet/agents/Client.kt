package com.htuy.jnet.agents

import com.htuy.jnet.messages.*
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

class Client(val hostAddr: String,
             val port: Int,
             val messageHandles: List<MessageHandler>,
             val initFunction: (ChannelHandlerContext) -> Unit = {},
             val cleanupFunction: (ChannelHandlerContext) -> Unit = {System.exit(0)},
             val password: String = "ADMINPASS123",
             val heartbeatFrequencyMillis : Int = -1) {

    var channel: Channel? = null
    val workerGroup = NioEventLoopGroup()


    fun connect(): ChannelFuture {
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
            LOGGER.error("Connect failed with some error. Channel failure : $res")
        }
        channel = res.channel()
        if(heartbeatFrequencyMillis != -1){
            startHeartbeats()
        }
        return res
    }

    fun startHeartbeats(){
        launch {
            while(channel?.isOpen ?: false){
                delay(heartbeatFrequencyMillis)
                LOGGER.trace{"Sending heartbeat"}
                sendMessage(HeartbeatMessage())
            }
        }
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

    fun shutdown(){
        workerGroup.shutdownGracefully()
        channel?.close()
    }
}


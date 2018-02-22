package com.htuy.jnet.protocol

import com.google.common.cache.CacheBuilder
import com.htuy.jnet.agents.Client
import com.htuy.jnet.agents.ReadHandler
import com.htuy.jnet.agents.Server
import com.htuy.jnet.agents.WriteHandler
import com.htuy.jnet.messages.ErrorMessage
import com.htuy.jnet.messages.ErrorType
import com.htuy.jnet.messages.FrameMessage
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel

class FrameManagerProtocol(val frameProvider : ((String) -> Any?)? = null) : Protocol {

    val framesMap = CacheBuilder.newBuilder().maximumSize(5).build<String, Any>()


    override fun installToClient(bootstrap: Bootstrap, client: Client) {
        client.installAfter("decoder", "framer", ReadHandler { ctx, msg ->
            if (msg is FrameMessage) {
                LOGGER.debug { "Adding a new object to the framesMap" }
                if (msg.frame == null) {
                    throw IllegalStateException("Remote sent an empty frame message response.")
                }
                framesMap.put(msg.hash, msg.frame)
            }
            false
        })
        client.installBefore("encoder", "framecache", WriteHandler { ctx, msg, promise ->
            if (msg is FrameMessage) {
                val frame = framesMap.getIfPresent(msg.hash)
                if (frame != null) {
                    ctx.fireChannelRead(FrameMessage(msg.hash, frame))
                    true
                } else {
                    false
                }

            } else {
                false
            }
        })
    }

    override fun installToServer(bootstrap: ServerBootstrap, server: Server) {
        if(frameProvider == null){
            throw IllegalStateException("Server requirs a valid frame provider")
        }
        server.installAfter("decoder","framemanager",{ReadHandler{ctx,msg ->
            if(msg is FrameMessage){
                val res = frameProvider?.invoke(msg.hash)
                if(res != null){
                    server.sendMessage(ctx.channel(),FrameMessage(msg.hash,res))
                }
                else{
                    server.sendMessage(ctx.channel(), ErrorMessage(ErrorType.UNKNOWN_FRAME, "Server did not have frame for ${msg.hash}"))
                }
                true
            }
            else{
                false
            }
        }})
    }
}
package com.htuy.jnet.protocol

import com.htuy.jnet.agents.Client
import com.htuy.jnet.agents.ReadHandler
import com.htuy.jnet.agents.Server
import com.htuy.jnet.messages.ErrorMessage
import com.htuy.jnet.messages.ErrorType
import com.htuy.jnet.messages.HandshakeMessage
import com.htuy.jnet.messages.Message
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel


class HandshakeProtocol(val password: String,val postHandshakeMessage : List<Message> = listOf()) : Protocol {

    override fun installToClient(bootstrap: Bootstrap, client: Client) {
        client.installAfter("decoder", "handshaker", ReadHandler { ctx, msg ->
            if (msg is HandshakeMessage && msg.confirmed) {
                ctx.pipeline().remove("handshaker")
                for(message in postHandshakeMessage){
                    client.sendMessage(message)
                }
            } else {
                client.sendMessage(ErrorMessage(ErrorType.BAD_HANDSHAKE, "No handshake or bad handshake sent"))
                client.shutdown()
            }
            true

        })
    }

    override fun installToServer(bootstrap: ServerBootstrap, server: Server) {
        server.installAfter("decoder", "handshaker", {ReadHandler { ctx, msg ->
            if (msg is HandshakeMessage && msg.password == password) {
                server.sendMessage(ctx.channel(),msg.confirm())
                ctx.pipeline().remove("handshaker")
            } else{
                server.sendMessage(ctx.channel(),ErrorMessage(ErrorType.BAD_HANDSHAKE,"No handshake or bad handshake."))
                ctx.channel().close()
            }
            true
        }})
    }

    override fun clientActive(chan: Channel, client: Client) {
        client.sendMessage(HandshakeMessage(password))
    }
}
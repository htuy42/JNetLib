package com.htuy.jnet.protocol

import com.htuy.jnet.agents.Client
import com.htuy.jnet.agents.Server
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel

interface Protocol{
    fun installToClient(bootstrap : Bootstrap, client : Client){

    }

    fun installToServer(bootstrap: ServerBootstrap, server: Server){

    }

    fun clientActive(chan : Channel, client : Client){

    }

    fun serverClientActive(chan : Channel, server : Server){

    }

    fun clientDeath(chan : Channel, client : Client){

    }

    fun serverClientDeath(chan : Channel, server : Server){

    }
}

class ProtocolBuilder : Protocol{
    val protocols : MutableList<Protocol> = ArrayList()

    override fun installToClient(bootstrap: Bootstrap, client: Client) {
        for(protocol in protocols){
            protocol.installToClient(bootstrap,client)
        }
    }

    override fun installToServer(bootstrap: ServerBootstrap, server: Server) {
        for(protocol in protocols){
            protocol.installToServer(bootstrap,server)
        }
    }

    override fun clientActive(chan: Channel, client: Client) {
        for(protocol in protocols){
            protocol.clientActive(chan,client)
        }
    }

    override fun serverClientActive(chan: Channel, server: Server) {
        for(protocol in protocols){
            protocol.serverClientActive(chan,server)
        }
    }

    override fun clientDeath(chan: Channel, client: Client) {
        for(protocol in protocols){
            protocol.clientDeath(chan,client)
        }
    }

    override fun serverClientDeath(chan: Channel, server: Server) {
        for(protocol in protocols){
            protocol.serverClientDeath(chan,server)
        }
    }

    fun with(add : Protocol) : ProtocolBuilder{
        protocols.add(add)
        return this
    }

    fun withHeartbeat(frequency : Int) : ProtocolBuilder{
        protocols.add(HeartbeatProtocol(frequency))
        return this
    }

    fun withHandshake(password : String) : ProtocolBuilder{
        protocols.add(HandshakeProtocol(password))
        return this
    }

    fun onClientActivation(call : (Channel,Client) -> Unit) : ProtocolBuilder{
        protocols.add(object : Protocol {
            override fun clientActive(chan: Channel, client: Client) {
                call(chan,client)
            }
        })
        return this
    }

}


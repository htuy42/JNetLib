package com.htuy.jnet.protocol

import com.htuy.jnet.agents.Client
import com.htuy.jnet.agents.ReadHandler
import com.htuy.jnet.agents.Server
import com.htuy.jnet.messages.HeartbeatMessage
import com.htuy.kt.stuff.LOGGER
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.time.Instant

//the amount of heartbeats that have to be missed before we kill the remote
val HEARTBEAT_MISSES_ALLOWABLE = 3

class HeartbeatProtocol(val frequencyMillis : Int) : Protocol{

    val allowedDelay = frequencyMillis * HEARTBEAT_MISSES_ALLOWABLE
    var lastHeartbeat : Long = 0
    override fun installToServer(bootstrap: ServerBootstrap, server: Server) {
        server.installAfter("decoder", "heartbeat", {ReadHandler{ctx,msg ->
            if(msg is HeartbeatMessage){
                LOGGER.debug{"Got a heartbeat message. Updating last received time."}
                lastHeartbeat = Instant.now().toEpochMilli()
                true
            } else{
                false
            }
        }})
    }

    override fun clientActive(chan: Channel, client: Client) {
        LOGGER.debug{"Starting to send heartbeat messages"}
        launch{
            while(chan.isOpen){
                delay(frequencyMillis)
                LOGGER.trace{"Sending a heartbeat message"}
                client.sendMessage(HeartbeatMessage())
            }
        }
    }

    override fun serverClientActive(chan: Channel, server: Server) {
        launch{
            lastHeartbeat = Instant.now().toEpochMilli() + 2 * frequencyMillis
            LOGGER.debug{"Starting server heartbeat monitoring"}
            launch{
                while(chan.isOpen){
                    val timeSince = Instant.now().toEpochMilli() - lastHeartbeat
                    if(timeSince < allowedDelay){
                        delay(allowedDelay - timeSince)
                        continue
                    } else{
                        LOGGER.debug{"Closing client due to failure to heartbeat"}
                        chan.close()
                        break
                    }
                }
            }
        }
    }

}
package com.htuy.jnet.agents

import com.htuy.jnet.messages.HeartbeatMessage
import com.htuy.kt.stuff.LOGGER
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.time.Instant

class HeartbeatMonitor(val allowedDelay : Int) : ChannelInboundHandlerAdapter(){
    //we are super forgiving on the first interval : connection might just not be set up yet
    var lastHeartbeat : Long = 0
    override fun channelActive(ctx: ChannelHandlerContext) {
        lastHeartbeat = Instant.now().toEpochMilli() + 2 * allowedDelay
        launch{
            while(ctx.channel().isOpen){
                val timeSince = Instant.now().toEpochMilli() - lastHeartbeat
                if(timeSince < allowedDelay){
                    delay(allowedDelay - timeSince)
                    continue
                } else{

                    LOGGER.warn{"Client being shutdown for failure to heartbeat."}
                    ctx.channel().close()
                    break
                }
            }
        }
        super.channelActive(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if(msg is HeartbeatMessage){
            LOGGER.trace{"Got valid heartbeat"}
            lastHeartbeat = Instant.now().toEpochMilli()
            ReferenceCountUtil.release(msg)
        }
        else{
            super.channelRead(ctx, msg)
        }
    }
}
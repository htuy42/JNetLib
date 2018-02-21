package com.htuy.jnet.agents

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class ServerTest {

    @Test
    fun catchRemoteShutdown() {
        var calls = 0


        val remote = Server(1234,
                            lastHandler = {object : ChannelInboundHandlerAdapter(){
                                override fun channelInactive(ctx: ChannelHandlerContext?) {
                                    calls += 1
                                }
                            }})
        remote.connect()
        val local = Client("localhost", 1234, lastHandlerList = listOf())
        local.connect()
        Thread.sleep(500)
        local.channel?.close()
        Thread.sleep(4000)
        assertEquals(calls, 1)
        remote.shutdown()
    }
}
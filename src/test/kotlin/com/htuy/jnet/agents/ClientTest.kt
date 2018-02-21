package com.htuy.jnet.agents

import com.htuy.jnet.messages.LogMessage
import com.htuy.jnet.messages.MessageTypeFunHandler
import com.htuy.jnet.protocol.Protocol
import com.htuy.jnet.protocol.ProtocolBuilder
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelInboundHandlerAdapter

import java.util.*
import java.util.concurrent.LinkedBlockingQueue

import kotlinx.coroutines.experimental.*

import kotlinx.coroutines.*
import kotlinx.coroutines.experimental.channels.Channel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

//the use of coroutines here is almost totally unnecessary. It just happens to work and be entertaining without really
//causing any harm

internal class ClientTest {

    var remote : Server? = null

    @BeforeEach
    fun setUp() {
        remote = Server(1234,ProtocolBuilder(),{ConnectionManager(listOf(EchoHandler()))})
        remote?.connect()
    }

    @AfterEach
    fun tearDown() {
        remote?.shutdown()
    }

    @Test
    fun connect() {
        //not tested at present
    }

    @Test
    fun sendMessage()= runBlocking<Unit> {
        val channel = Channel<Int>()
        var calls = 0
        val sentMessage = LinkedBlockingQueue<LogMessage>()
        val client = Client("localhost",1234,ProtocolBuilder(),ConnectionManager(listOf(MessageTypeFunHandler(LogMessage::class.java, { ctx, msg ->
            val correct = sentMessage.poll()
            assert(correct == msg)
            calls+= 1
        }))))
        val future = client.connect()
        for(x in 0 .. 100){
            val msg = LogMessage(x.toString())
            sentMessage.add(msg)
            client.sendMessage(msg)
        }

        Thread.sleep(3500)
        assertEquals(calls,101)
        client.shutdown()
    }

    @Test
    fun shutdown() {
        var calls = 0
        val remote = Server(1235, ProtocolBuilder(),{object : ChannelInboundHandlerAdapter(){
            override fun channelInactive(ctx: ChannelHandlerContext?) {
                calls += 1
            }

            override fun channelActive(ctx: ChannelHandlerContext?) {
                calls += 1
            }
        }})
        remote.connect()
        val local = Client("localhost", 1235, ProtocolBuilder(),object : ChannelInboundHandlerAdapter(){
            override fun channelInactive(ctx: ChannelHandlerContext?) {
                calls += 1
            }

            override fun channelActive(ctx: ChannelHandlerContext?) {
                calls += 1
            }
        })
        local.connect()
        Thread.sleep(500)
        assertEquals(calls, 2)
        local.shutdown()
        //shutdown actually takes a bit: this interval winds up having to be kind of long
        Thread.sleep(4500)
        assertEquals(calls, 4)
    }

}
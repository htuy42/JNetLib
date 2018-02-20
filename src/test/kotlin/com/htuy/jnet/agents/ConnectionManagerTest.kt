package com.htuy.jnet.agents

import com.htuy.jnet.messages.*
import com.htuy.kt.stuff.SingletonFactory
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue


internal class ConnectionManagerTest {

    var remote : Server? = null

    @BeforeEach
    fun setUp() {
        remote = Server(1234, SingletonFactory(listOf(EchoHandler())))
        remote?.connect()
    }

    @AfterEach
    fun tearDown() {
        remote?.shutdown()
    }

    class AllHandler(val doWith : (Any?) -> Unit) : MessageHandler{
        override fun handles(msg: Any?): Boolean {
            return true
        }

        override fun handle(ctx: ChannelHandlerContext, msg: Any?) {
            doWith(msg)
        }

        override var cleanupRequired = true
    }

    @Test
    fun channelRead() = runBlocking<Unit> {
        val received = LinkedBlockingQueue<Message>()
        val client = Client("localhost",1234,listOf(AllHandler{
            received.add(it as Message)
        }))
        client.connect()

        for(x in 0 .. 100){
            when {
                x % 3 == 0 -> client.sendMessage(ErrorMessage(ErrorType.MODULE_NOT_INSTALLED, "x"))
                x % 3 == 1 -> client.sendMessage(ModuleMessage(listOf("dog123")))
                else -> client.sendMessage(HandshakeMessage("wherewithal", true))
            }
        }
        var x = 0
        delay(2000)
        for(msg in received){
            when {
                x % 3 == 0 -> assert(ErrorMessage(ErrorType.MODULE_NOT_INSTALLED, "x") == msg)
                x % 3 == 1 -> assert(ModuleMessage(listOf("dog123")) == msg)
                else -> assert(HandshakeMessage("wherewithal", true) == msg)
            }
            x += 1
        }
    }

}
package com.htuy.jnet.agents

import com.htuy.jnet.messages.LambdaLifecycleHandler
import com.htuy.kt.stuff.SingletonFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class ServerTest {

    @Test
    fun catchRemoteShutdown() {
        var calls = 0


        val remote = Server(1234,
                            SingletonFactory(listOf()),
                            cleanupFunctionMaker = SingletonFactory(LambdaLifecycleHandler { calls += 1 }))
        remote.connect()
        val local = Client("localhost", 1234, listOf())
        local.connect()
        Thread.sleep(500)
        local.channel?.close()
        Thread.sleep(4000)
        assertEquals(calls, 1)
        remote.shutdown()
    }
}
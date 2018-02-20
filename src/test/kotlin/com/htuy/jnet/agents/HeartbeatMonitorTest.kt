package com.htuy.jnet.agents

import com.htuy.kt.stuff.SingletonFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HeartbeatMonitorTest{

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

    @Test
    fun normalFunctioning(){
        val client = Client("localhost",1234,listOf(),heartbeatFrequencyMillis = 500)
        Thread.sleep(100)
        remote?.installAfter("decoder","heartbeat",SingletonFactory(HeartbeatMonitor(1500)))
        val future = client.connect()

        Thread.sleep(8000)
        assertTrue(future.channel().isOpen)
        client.shutdown()
    }

    @Test
    fun shutdownFunctioning(){
        val client = Client("localhost",1234,listOf(),heartbeatFrequencyMillis = 1600)
        Thread.sleep(100)
        remote?.installAfter("decoder","heartbeat",SingletonFactory(HeartbeatMonitor(1500)))
        val future = client.connect()

        Thread.sleep(8000)
        assertFalse(future.channel().isOpen)
        client.shutdown()
    }

}
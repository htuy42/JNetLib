package com.htuy.jnet.agents

import com.htuy.jnet.protocol.ProtocolBuilder
import com.htuy.jnet.protocol.STANDARD_WORKER_PROTOCOL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HeartbeatMonitorTest{

    var remote : Server? = null

    @BeforeEach
    fun setUp() {
        remote = Server(1234, STANDARD_WORKER_PROTOCOL, lastHandlerList = {listOf(EchoHandler())})
        remote?.connect()
    }

    @AfterEach
    fun tearDown() {
        remote?.shutdown()
    }

    @Test
    fun normalFunctioning(){
        val client = Client("localhost", 1234, STANDARD_WORKER_PROTOCOL,lastHandlerList = listOf())
        Thread.sleep(100)
        val future = client.connect()
        Thread.sleep(8000)
        assertTrue(future.channel().isOpen)
        client.shutdown()
    }

    @Test
    fun shutdownFunctioning(){
        val client = Client("localhost",1234,ProtocolBuilder().withHeartbeat(1600),lastHandlerList = listOf())
        Thread.sleep(100)
        val future = client.connect()
        Thread.sleep(8000)
        assertFalse(future.channel().isOpen)
        client.shutdown()
    }

}
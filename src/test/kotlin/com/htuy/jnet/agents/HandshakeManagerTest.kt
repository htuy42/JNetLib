package com.htuy.jnet.agents

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.sql.Time
import java.time.Instant
import java.util.concurrent.TimeUnit


internal class HandshakeManagerTest {


    @Test
    fun wrongPassClient() {
        //there's no "wrong pass server." The passwords either match or they don't, and since the client is the
        //connector it is assumed that the client is at fault. When this happens the server just keeps chugging along.
        val remote = Server(1234, {listOf(EchoHandler())})
        remote.connect()
        val local = Client("localhost", 1234, listOf(), password = "321WRONGPASS")
        val con = local.connect()
        Thread.sleep(500)
        assertTrue(con.channel().closeFuture().isDone)
        remote.shutdown()
    }

    @Test
    fun correctPassClient() {
        val remote = Server(1234, {listOf(EchoHandler())})
        remote.connect()
        val local = Client("localhost", 1234, listOf(), password = "ADMINPASS123")
        val con = local.connect()
        Thread.sleep(500)
        assertFalse(con.channel().closeFuture().isDone)
        remote.shutdown()
        local.shutdown()
    }



}
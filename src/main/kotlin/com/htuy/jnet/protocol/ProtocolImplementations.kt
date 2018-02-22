package com.htuy.jnet.protocol

import com.htuy.jnet.messages.WorkerPowerMessage
import com.htuy.kt.stuff.LOGGER

val STANDARD_CLIENT_PROTOCOL = ProtocolBuilder().withHandshake("ADMIN123")

private val pool_worker_shared_protocol = {ProtocolBuilder()
        .with(
                HandshakeProtocol(
                        "ADMIN123", listOf(WorkerPowerMessage(Runtime.getRuntime().availableProcessors()))))
        .withHeartbeat(500)}

val WORKER_TO_POOL_PROTOCOL = pool_worker_shared_protocol()
        .with(FrameManagerProtocol())

fun POOL_TO_WORKER_PROTOCOL(frameProvider : (String) -> Any?) : Protocol {
    return (pool_worker_shared_protocol().with(FrameManagerProtocol(frameProvider = frameProvider)))
}

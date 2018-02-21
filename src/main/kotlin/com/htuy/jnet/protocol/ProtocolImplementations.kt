package com.htuy.jnet.protocol

import com.htuy.jnet.messages.WorkerPowerMessage

val STANDARD_CLIENT_PROTOCOL = ProtocolBuilder().withHandshake("ADMIN123")

val STANDARD_WORKER_PROTOCOL = ProtocolBuilder()
        .with(
                HandshakeProtocol(
                        "ADMIN123", listOf(WorkerPowerMessage(Runtime.getRuntime().availableProcessors()))))
        .withHeartbeat(500)

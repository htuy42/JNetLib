package com.htuy.jnet.messages

import java.io.Serializable

sealed class Message : Serializable

data class ModuleMessage(val moduleNames: List<String>, val update:Boolean = false) : Message()
data class HandshakeMessage(val password: String,
                            val confirmed: Boolean = false) : Message() {
    fun confirm(): HandshakeMessage {
        return HandshakeMessage(password, true)
    }
}

data class FrameMessage(val hash : String, val frame : Any?)

data class LogMessage(val log : String) : Message()

data class WorkMessage(val work: WorkBlock,
                       val answer: Any? = null) : Message()

data class SubWorkMessage(val subWork: WorkSubunit,
                          val answer: Any? = null) : Message()


data class ErrorMessage(val error: ErrorType,
                        val description: String) : Message()

data class WorkerPowerMessage(val amount : Int) : Message()

class HeartbeatMessage : Message()

enum class ErrorType {
    MODULE_NOT_INSTALLED,

    BAD_HANDSHAKE,

    SHUTTING_DOWN
}

data class LifecycleMessage(val event: LifecycleEvent) : Message()

enum class LifecycleEvent {
    SHUTDOWN
}
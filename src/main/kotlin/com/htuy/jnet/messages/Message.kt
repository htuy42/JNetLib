package com.htuy.jnet.messages

import java.io.Serializable

sealed class Message : Serializable

data class ModuleMessage(val moduleNames: List<String>) : Message()
data class HandshakeMessage(val password: String,
                            val confirmed: Boolean = false) : Message() {
    fun confirm(): HandshakeMessage {
        return HandshakeMessage(password, true)
    }
}

data class LogMessage(val log : String) : Message()

data class WorkMessage(val work: WorkBlock,
                       val answer: Any? = null) : Message()

data class SubWorkMessage(val subWork: WorkSubunit,
                          val answer: Any? = null) : Message()


data class ErrorMessage(val error: ErrorType,
                        val description: String) : Message()


enum class ErrorType {
    MODULE_NOT_INSTALLED,

    BAD_HANDSHAKE
}

data class LifecycleMessave(val event: LifecycleEvent)

enum class LifecycleEvent {
    SHUTDOWN
}
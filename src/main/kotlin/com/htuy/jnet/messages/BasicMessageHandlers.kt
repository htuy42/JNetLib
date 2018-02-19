package com.htuy.jnet.messages

import com.htuy.kt.stuff.LOGGER
import kotlin.system.exitProcess

fun DoRequestedWorkHandler(): MessageHandler {
    return MessageTypeFunHandler(SubWorkMessage::class.java, { ctx, msg ->
        LOGGER.trace { "Performing requested work" }
        ctx.writeAndFlush(msg.subWork.perform())
    })
}

fun ObeyLifecycleRequestHandler(): MessageHandler {
    return MessageTypeFunHandler(LifecycleMessave::class.java, { ctx, msg ->
        LOGGER.warn { "Received lifecycle event ${msg.event.name}. Obeying." }
        when (msg.event) {
            LifecycleEvent.SHUTDOWN -> exitProcess(0)
        }
    })
}

fun GenericErrorHandler(): MessageHandler {
    return MessageTypeFunHandler(ErrorMessage::class.java, { ctx, msg ->
        LOGGER.error { "Receveived error ${msg.error} with description ${msg.description}" }
    })
}

fun RequestWorkReceipt(callback: (WorkMessage) -> Unit): MessageHandler {
    return MessageTypeFunHandler(WorkMessage::class.java, { ctx, msg ->
        LOGGER.trace { "Work result received. Running callback." }
        callback(msg)
    })
}
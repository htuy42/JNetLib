package com.htuy.jnet.messages

import com.htuy.kt.stuff.LOGGER
import java.util.concurrent.ExecutorService
import kotlin.system.exitProcess

fun DoRequestedWorkHandler(): MessageHandler {
    return MessageTypeFunHandler(SubWorkMessage::class.java, { ctx, msg ->
        LOGGER.trace { "Performing requested work" }
        ctx.writeAndFlush(msg.subWork.perform())
    })
}

fun MultiDoRequestedWorkHandler(pool: ExecutorService): MessageHandler {
    return MessageTypeFunHandler(SubWorkMessage::class.java, { ctx, msg ->
        LOGGER.trace { "Submitting requested work to work pool." }
        pool.submit({
                        val result = msg.subWork.perform()
                        LOGGER.trace { "Finished doing work in pool. Sending." }
                        ctx.writeAndFlush(result)
                    })
    })
}

fun ObeyLifecycleRequestHandler(): MessageHandler {
    return MessageTypeFunHandler(LifecycleMessage::class.java, { ctx, msg ->
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
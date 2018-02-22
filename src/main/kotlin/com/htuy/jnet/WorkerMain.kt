package com.htuy.jnet

import com.htuy.jnet.agents.Client
import com.htuy.jnet.messages.*
import com.htuy.jnet.modules.ModuleManager
import com.htuy.jnet.modules.ModuleMessageLoadHandler
import com.htuy.jnet.modules.SiteInstaller
import javafx.concurrent.Worker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun WorkerMain(args : Array<String>){
    val hostAddr: String = args[0]
    val port: Int = args[1].toInt()
    val password = args[2]
    val moduleManager = ModuleManager("modules/", SiteInstaller())
    val numProcessors = Runtime.getRuntime().availableProcessors()
    val pool = Executors.newFixedThreadPool(numProcessors)
    val messageHandles: List<MessageHandler> = listOf(ModuleMessageLoadHandler(moduleManager),
                                                      ObeyLifecycleRequestHandler(),
                                                      MultiDoRequestedWorkHandler(pool))


    val worker = Client(hostAddr, port, messageHandles, initFunction = {ctx ->
        ctx.writeAndFlush(WorkerPowerMessage(numProcessors))
    }, password = password)
    worker.connect()
            .sync()
            .channel()
            .closeFuture()
            .sync()
}

fun main(args: Array<String>) {
    WorkerMain(args)
}
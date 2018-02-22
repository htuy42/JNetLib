package com.htuy.jnet

import com.htuy.jnet.agents.Client
import com.htuy.jnet.agents.ConnectionManager
import com.htuy.jnet.messages.*
import com.htuy.jnet.modules.ModuleManager
import com.htuy.jnet.modules.ModuleMessageLoadHandler
import com.htuy.jnet.modules.SiteInstaller
import com.htuy.jnet.protocol.WORKER_TO_POOL_PROTOCOL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val hostAddr: String = args[0]
    val port: Int = args[1].toInt()
    val moduleManager = ModuleManager("modules/", SiteInstaller())
    val numProcessors = Runtime.getRuntime().availableProcessors()
    val pool = Executors.newFixedThreadPool(numProcessors)
    val messageHandles: List<MessageHandler> = listOf(ModuleMessageLoadHandler(moduleManager),
                                                      ObeyLifecycleRequestHandler(),
                                                      MultiDoRequestedWorkHandler(pool))


    val worker = Client(hostAddr, port, WORKER_TO_POOL_PROTOCOL,ConnectionManager(messageHandles))
    worker.connect()
            .sync()
            .channel()
            .closeFuture()
            .sync()
}
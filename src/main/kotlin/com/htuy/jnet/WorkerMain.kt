package com.htuy.jnet

import com.htuy.jnet.agents.Client
import com.htuy.jnet.messages.DoRequestedWorkHandler
import com.htuy.jnet.messages.MessageHandler
import com.htuy.jnet.messages.ObeyLifecycleRequestHandler
import com.htuy.jnet.modules.ModuleManager
import com.htuy.jnet.modules.ModuleMessageLoadHandler
import com.htuy.jnet.modules.SiteInstaller

fun main(args: Array<String>) {
    val hostAddr: String = args[0]
    val port: Int = args[1].toInt()
    val password = args[2]
    val moduleManager = ModuleManager("modules/", SiteInstaller())

    val messageHandles: List<MessageHandler> = listOf(ModuleMessageLoadHandler(moduleManager),
                                                      ObeyLifecycleRequestHandler(),
                                                      DoRequestedWorkHandler())


    val worker = Client(hostAddr, port, messageHandles, password = password)
    worker.connect()
            .sync()
            .channel()
            .closeFuture()
            .sync()
}
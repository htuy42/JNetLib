package com.htuy.jnet.modules

import com.htuy.jnet.messages.MessageHandler
import com.htuy.jnet.messages.ModuleMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import kotlin.reflect.full.primaryConstructor

class ModuleMessageLoadHandler(val manager: ModuleManager)
    : MessageHandler {
    override var cleanupRequired = false
    override fun handle(ctx: ChannelHandlerContext,
                        msg: Any?) {
        msg as ModuleMessage
        manager.loadAsNeeded(msg.moduleNames)
    }

    override fun handles(msg: Any?): Boolean {
        return msg != null && msg is ModuleMessage
    }
}


//todo should catch the message, send load requests for it, and hang until the stuff has been loaded
class ModuleMessageRequestHandler : ChannelOutboundHandlerAdapter() {
    override fun write(ctx: ChannelHandlerContext?,
                       msg: Any?,
                       promise: ChannelPromise?) {

    }
}

class ModuleManager(val modulesLocalLocation: String,
                    val modulesFetcher: ModuleInstaller) {
    val loadedModules = HashSet<String>()

    val moduleObjects = HashMap<String, Module>()

    fun loadAsNeeded(toLoad: List<String>) {
        toLoad
                .filterNot { loadedModules.contains(it) }
                .forEach {
                    installModuleIfNeeded(it, modulesFetcher, modulesLocalLocation)
                    loadedModules.add(it)
                }
    }

    fun getModule(moduleName: String): Module {
        if (moduleObjects.containsKey(moduleName)) {
            return moduleObjects.get(moduleName)!!
        }
        loadAsNeeded(listOf(moduleName))
        val moduleKclass = Class.forName(REQUIRED_PACKAGE_NAME + "${moduleName}Module")
                .kotlin
        val moduleObject = moduleKclass.primaryConstructor?.call()
                ?: throw IllegalStateException("Problem getting module")
        moduleObject as Module
        moduleObjects.put(moduleName, moduleObject)
        return moduleObject
    }

}
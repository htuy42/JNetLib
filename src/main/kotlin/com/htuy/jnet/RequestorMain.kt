package com.htuy.jnet

import com.htuy.jnet.agents.Client
import com.htuy.jnet.messages.*
import com.htuy.jnet.modules.ModuleManager
import com.htuy.jnet.modules.SiteInstaller
import com.htuy.kt.stuff.REPL
import com.htuy.kt.stuff.StringCommandsRegistry

fun runRequestorREPL(client: Client) {
    val moduleManager = ModuleManager("modules/", SiteInstaller())
    val registry = StringCommandsRegistry()

    registry.stringRegister("mod_run", {
        val lst = it.split(" ")
        if (lst.size < 1) {
            println("Must give a module name")
        } else {
            val mod = moduleManager.getModule(lst[0])
            val msg = mod.messageFromCommand(lst.drop(1).joinToString(" "))
            client.sendMessage(msg)
        }
    })

    registry.stringRegister("remote_shutdown", {
        client.sendMessage(LifecycleMessage(LifecycleEvent.SHUTDOWN))
    })

    registry.stringRegister("global_install", {
        val input = it.split(" ")
        val update = it.split(" ")[0] == "update"
        if (update) {
            input.drop(1)
        }
        client.sendMessage(ModuleMessage(input,update))
        moduleManager.loadAsNeeded(input, false)
    })


    val repl = REPL(registry)
    repl.run()


}

fun ClientMain(args : Array<String>){
    val hostAddr: String = args[0]
    val port: Int = args[1].toInt()


    val client = Client(hostAddr,
                        port,
                        listOf(GenericErrorHandler(),
                               RequestWorkReceipt({ println(it.answer.toString()) })),
                        password = "ADMINPASS123")
    client.connect()
            .sync()
    runRequestorREPL(client)
}


fun main(args: Array<String>) {
    ClientMain(args)
}
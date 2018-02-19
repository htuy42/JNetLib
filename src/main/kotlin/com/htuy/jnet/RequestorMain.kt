package com.htuy.jnet

import com.htuy.jnet.agents.Client
import com.htuy.jnet.messages.GenericErrorHandler
import com.htuy.jnet.messages.ModuleMessage
import com.htuy.jnet.messages.RequestWorkReceipt
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

    registry.stringRegister("remote_install", {
        val toInstall = it.split(" ")
        client.sendMessage(ModuleMessage(toInstall))
    })

    registry.stringRegister("global_install", {
        val toInstall = it.split(" ")
        client.sendMessage(ModuleMessage(toInstall))
        moduleManager.loadAsNeeded(toInstall)
    })


    val repl = REPL(registry)
    repl.run()


}


fun main(args: Array<String>) {
    val hostAddr: String = args[0]
    val port: Int = args[1].toInt()
    val password = args[2]


    val client = Client(hostAddr,
                        port,
                        listOf(GenericErrorHandler(),
                               RequestWorkReceipt({ println(it.answer.toString()) })),
                        password = password)
    client.connect()
            .sync()
    runRequestorREPL(client)


}
package com.htuy.kt.stuff

interface CommandsRegistry <T> {
    fun register(toRegister: (T) -> Boolean)

    fun handle(toHandle: T)
}

open class AnyCommandsRegistry <T> : CommandsRegistry<T> {
    val handlers : MutableCollection<(T) -> Boolean> = ArrayList()
    override fun register(toRegister : (T) -> Boolean) {
        handlers.add(toRegister)
    }
    override fun handle(toHandle : T){
        for(handler in handlers){
            if(handler(toHandle)){
                return
            }
        }
        println("Didn't know how to handle that input. Perhaps there was a typo?")
    }
}

class StringCommandsRegistry : AnyCommandsRegistry<String>(){
    val mappedHandlers : MutableMap<String,(String) -> Unit> = HashMap()

    fun stringRegister(mapTo : String, toRegister : (String) -> Unit){
        mappedHandlers[mapTo] = toRegister
    }

    override fun handle(toHandle: String) {
        val firstWord = toHandle.split(" ").firstOrNull()
        if(firstWord in mappedHandlers){
            mappedHandlers[firstWord]?.invoke(toHandle.split(" ").drop(1).joinToString(" "))
        }
        else{
            super.handle(toHandle)
        }
    }
}
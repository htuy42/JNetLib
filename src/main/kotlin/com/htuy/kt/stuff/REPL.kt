package com.htuy.kt.stuff

import java.io.IOException
import jdk.nashorn.internal.runtime.ScriptingFunctions.readLine
import java.io.BufferedReader
import java.io.InputStreamReader



class REPL(val registry : CommandsRegistry<String>){
    fun run(){
        try {
            // Get the object of DataInputStream
            val isr = InputStreamReader(System.`in`)
            val br = BufferedReader(isr)
            var line : String? = br.readLine()
            while (line != null) {
                registry.handle(line)
                line = br.readLine()
            }
            isr.close()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }
}
package com.htuy.jnet

fun main(args : Array<String>){
    val toRun = args[0]
    val subCallArgs = args.sliceArray(1 until args.size)
    when(toRun){
        "worker" -> WorkerMain(subCallArgs)
        "pool" -> PoolMain(subCallArgs)
        "client" -> ClientMain(subCallArgs)
    }
}
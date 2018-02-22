package com.htuy.jnet

import com.htuy.jnet.agents.Pool


fun PoolMain(args : Array<String>){
    val pool: Pool = Pool(1234, 5678)
    pool.launch()
}

fun main(args: Array<String>) {
    PoolMain(args)
}
package com.htuy.jnet

import com.htuy.jnet.agents.Pool


fun main(args: Array<String>) {
    val pool: Pool = Pool(1234, 8765)
    pool.launch()
}
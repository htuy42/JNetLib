package com.htuy.kt.stuff

interface Factory <out T>{
    fun getInstance() : T
}

class FunctionFactory <out T> (private val func : () -> T) : Factory<T> {
    override fun getInstance() : T{
        return func()
    }
}

class SingletonFactory<out T> (private val singleton : T): Factory<T>{
    override fun getInstance(): T {
        return singleton
    }
}
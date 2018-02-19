package com.htuy.kt.stuff

import com.google.common.hash.Hashing
import java.io.Serializable
import java.nio.charset.Charset

interface ShaHashable {
    fun shaHashToString() : String
}

data class StringHashable(val internal : String) : ShaHashable, Serializable{
    override fun shaHashToString(): String {
        return Hashing.sha256().hashString(internal, Charset.defaultCharset()).toString()
    }
}
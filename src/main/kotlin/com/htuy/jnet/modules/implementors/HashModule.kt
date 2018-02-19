//package com.com.htuy.jnet.modules.implementors
//
//import com.google.common.hash.Hashing
//import com.com.htuy.jnet.messages.*
//import com.com.htuy.jnet.modules.Module
//import com.com.htuy.kt.stuff.ShaHashable
//import com.com.htuy.kt.stuff.StringHashable
//import java.nio.charset.Charset
//
//class HashModule : Module {
//    override fun checkLoadedProperly() {
//        println("Hash module loaded")
//    }
//
//    override fun messageFromCommand(command: String): WorkMessage {
//        val commandElts = command.split(" ")
//        val data = commandElts[0]
//        val upTo = commandElts[1].toInt()
//        return WorkMessage(HashWork(data,upTo),null)
//    }
//}
//
//data class HashWork(val toHash : String, val upTo : Int) : WorkBlock{
//    override val modulesRequired: List<String> = listOf("Hash")
//    override val frame = StringHashable(toHash)
//
//    override fun splitWork(): List<WorkSubunit> {
//        val numIntervals = Math.max(100,upTo / 5000)
//        val intervalSize = upTo / numIntervals
//        return (0 until numIntervals).map{
//            if(it == numIntervals){
//                HashSubwork(frame,Pair(it*intervalSize,upTo),frame.shaHashToString())
//            }
//            else{
//                HashSubwork(frame,Pair(it*intervalSize,(it+1)*intervalSize),frame.shaHashToString())
//            }
//        }
//    }
//    override fun recombineWork(doneWork: List<WorkSubunit>): Message {
//        var bestVal = -1
//        var scoreOfBest = Int.MAX_VALUE
//        for(work in doneWork){
//            work as HashSubwork
//            if(work.scoreOfBest < scoreOfBest){
//                scoreOfBest = work.scoreOfBest
//                 bestVal = work.bestVal
//            }
//        }
//        return WorkMessage(this,Pair(bestVal,scoreOfBest))
//    }
//}
//
//data class HashSubwork(override val frame : StringHashable,
//                       override val work : Pair<Int,Int>,
//                       override val frameIdentifier : String) : WorkSubunit{
//    override val modulesRequired: List<String> = listOf("Hash")
//    var bestVal : Int = Int.MAX_VALUE
//    var scoreOfBest : Int = Int.MAX_VALUE
//    override fun perform(): Message {
//        for(x in work.first .. work.second){
//            val thisScore = Math.abs(Hashing.sha512().hashString(frame.internal + x, Charset.defaultCharset()).asInt() % 100000)
//            if(thisScore < scoreOfBest){
//                bestVal = x
//                scoreOfBest = thisScore
//            }
//        }
//        return SubWorkMessage(this,Pair<Int,Int>(bestVal,scoreOfBest))
//    }
//}
//

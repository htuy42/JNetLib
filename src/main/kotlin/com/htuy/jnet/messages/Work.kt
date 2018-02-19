package com.htuy.jnet.messages

import com.htuy.kt.stuff.ShaHashable
import java.io.Serializable

interface WorkBlock : Serializable {

    //todo a manager that can be inserted into a pipeline that deals with frames
    val modulesRequired: List<String>

    val frame: ShaHashable


    fun splitWork(): List<WorkSubunit>


    //clearly, this is invited to throw an error if given invalid work /
    //work it didn't make itself
    fun recombineWork(doneWork: List<WorkSubunit>): Message
}

interface WorkSubunit : Serializable {

    val modulesRequired: List<String>

    val frameIdentifier: String

    //describes what to do on the frame to generate the answer.
    //in most cases this is probably something like a list
    //of coordinates
    val work: Any


    //the frame. Note that a WorkSubunit always needs to know this,
    //but that doesn't necessarily mean it always has to be sent.
    //again, some sort of decoder for handling ths is needed
    val frame: ShaHashable?


    //ought to return a message with the "answer" to the unit of work
    fun perform(): Message

}
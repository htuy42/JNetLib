package com.htuy.jnet.modules

import com.htuy.jnet.messages.WorkMessage
import kotlin.reflect.full.primaryConstructor

interface Module {
    //this is expected to perform any operations necessary to load the given module object,
    //to confirm that is is properly set up.
    //basically, create a copy of each of the relevant objects to a module and make sure that works correctly.
    //If there is a failure, an exception should be thrown. This is because in the vast majority of cases
    //failures will be to class load errors, which throw an error either way.
    fun checkLoadedProperly()

    fun messageFromCommand(command: String): WorkMessage

}


val REQUIRED_PACKAGE_NAME = "com.htuy.jnet.modules.implementors."

//we have to assume that a module is named following convention, ie it is stored in a file named <moduleName>Module.kt
//at the moment we assume that all modules can be found in jnet.modules.implementors
fun checkForModule(moduleName: String): Boolean {
    try {
        val moduleKclass = Class.forName(REQUIRED_PACKAGE_NAME + "${moduleName}Module")
                .kotlin
        val moduleObject = moduleKclass.primaryConstructor?.call() ?: return false
        moduleObject as Module
        try {
            moduleObject.checkLoadedProperly()
        } catch (e: Exception) {
            return false
        }
    } catch (e: Exception) {
        return false
    }
    return true
}


fun installModuleIfNeeded(moduleName: String,
                          installer: ModuleInstaller,
                          pathToModules: String,
                          update : Boolean) {
    if(update){
        installer.update(moduleName,pathToModules)
    }
    else if(!checkForModule(moduleName)){
        installer.install(moduleName, pathToModules)
        if (!checkForModule(moduleName)) {
            throw IllegalStateException("Failed to install module $moduleName")
        }
    }

}
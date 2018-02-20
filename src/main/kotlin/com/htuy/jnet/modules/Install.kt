package com.htuy.jnet.modules

import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.net.URLClassLoader

interface ModuleInstaller {
    fun install(toInstall: String,
                pathToModules: String)

    fun update(toUpdate : String, pathToModules: String)
}

fun loadLibraryFromJar(path: String) {
    val file = File(path)
    val url = file.toURI()
            .toURL()
    val classLoader = ClassLoader.getSystemClassLoader() as URLClassLoader
    val method = classLoader::class.java.superclass.getDeclaredMethod("addURL", URL::class.java)
    method.isAccessible = true
    method.invoke(classLoader, url)
}

class SiteInstaller : ModuleInstaller {
    val SITE_BASE_URL = "https://www.jpattiz.com/modules/"
    override fun install(toInstall: String,
                         pathToModules: String) {
        loadLibraryFromJar(downloadJar(toInstall, pathToModules))
    }

    override fun update(toUpdate: String, pathToModules: String) {
        loadLibraryFromJar(downloadJar(toUpdate,pathToModules,force = true))
    }

    fun downloadJar(toInstall: String,
                    pathToModules: String,
                    force : Boolean = false): String {
        val path = pathToModules + "$toInstall.jar"
        val outFile = File(path)
        if(!force){
            if(outFile.exists()){
                return path
            }
        }
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { hostname, sslSession -> true }
        FileUtils.copyURLToFile(URL(SITE_BASE_URL + "$toInstall.jar"), outFile)
        return path
    }
}
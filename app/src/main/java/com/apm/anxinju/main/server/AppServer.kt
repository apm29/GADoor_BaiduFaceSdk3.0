package com.apm.anxinju.main.server

import android.util.Log
import com.apm.anxinju.main.App
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

/**
 *  author : ciih
 *  date : 2019-08-21 15:33
 *  description :
 */
class AppServer(port: Int) : NanoHTTPD(port) {

    companion object {
        const val TAG = "AppServer"
        const val MIME_CSS = "text/css"
        const val MIME_JPEG = "image/jpeg"
        const val MIME_PNG = "image/png"
        const val MIME_JSON = "application/json"
    }

    init {
        Log.d(TAG, "Start AppServer on Port $port")
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    override fun serve(session: IHTTPSession): Response {
        Log.e(TAG, session.uri)
        return when (session.uri) {
            //Main
            "/", "/index_home.html" -> {
                val data: InputStream = App.contextGlobal.assets.open("index_home.html")
                newChunkedResponse(Response.Status.OK, MIME_HTML, data)
            }
            // data interfaces
            "/databaseList" -> {
                newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, gson.toJson(arrayListOf<Any>()))
            }
            "/logList" -> {
                val page = session.parms["page"]?.toInt() ?: 0
                val pageSize = session.parms["pageCount"]?.toInt() ?: 50
                val pagedList: List<Any> =arrayListOf<Any>()
                newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, gson.toJson(pagedList))
            }
            "/logListPageCount" -> {
                val pageSize = session.parms["pageCount"]?.toInt() ?: 50
                newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, gson.toJson(0))
            }


            //file interfaces
            else -> {
                val start = session.uri.indexOf('/') + 1
                val end = session.uri.length
                val route = session.uri.substring(start, end)
                val data: InputStream = App.contextGlobal.assets.open(route)
                val mimeType = when (route.substring(route.lastIndexOf("."))) {
                    ".css" -> MIME_CSS
                    ".html" -> MIME_HTML
                    ".png" -> MIME_PNG
                    ".jpg" -> MIME_JPEG
                    else -> MIME_PLAINTEXT
                }
                newChunkedResponse(Response.Status.OK, mimeType, data)
            }
        }
    }



}
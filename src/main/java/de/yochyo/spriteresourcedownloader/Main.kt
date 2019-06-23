package de.yochyo.spriteresourcedownloader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.lang.Exception
import java.net.URL

lateinit var host: String
fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val builder = StringBuilder()
        for (a in args)
            builder.append(a)
        val url = builder.toString().filter { it != '"' && it != '\'' }
        host = "https://${URL(url).host}"
        val doc = Jsoup.connect(url).get()
        val sheetsNumber = getSheetsNumber(doc)
        val links = getLinksToSprite(doc)
        if(sheetsNumber != links.size) println("error in program, not all sprites are downloaded")
        var downloaded = 1
        val threadAmount = 5
        for(i in 0 until threadAmount){
            Thread{
                for(index in i until sheetsNumber step threadAmount){
                    try {
                        val doc = Jsoup.connect(host + links[index]).get()
                        val bytes = downloadImageByUrl(doc)
                        val name = getImageNameByUrl(doc)
                        if(bytes != null && name != null){
                            saveFile("${index+1} - $name", bytes)
                            println("Downloaded $downloaded/$sheetsNumber")
                        }else println("Error downloading $index")
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                    downloaded++
                }
            }.start()
        }

    }
}

fun getSheetsNumber(doc: Document): Int {
    var sheetsNumber = -1
    for (sheetRow in doc.getElementsByClass("altrow0")) {
        for (e in sheetRow.allElements)
            if (e.text() == "Sheets") {
                sheetsNumber = sheetRow.allElements.last().text().toInt()
            }
    }
    return sheetsNumber
}

fun getLinksToSprite(doc: Document): ArrayList<String> {
    val links = ArrayList<String>(getSheetsNumber(doc))
    val spriteContainers = doc.getElementsByClass("updatesheeticons")
    for (item in spriteContainers) {
        for (i in item.getElementsByAttribute("style"))
            links += i.attr("href")
    }
    return links
}

fun getImageUrl(doc: Document): String? {
    try {
        for (e in doc.allElements) {
            if (e.text() == "Download this Sheet"){
                val s = host + e.allElements.last().attr("href") //todo ist das hier jetzt der richtige link?
                return s
            }
        }
    }catch (e: Exception){}
    return null
}

fun downloadImageByUrl(doc: Document): ByteArray? {
    try {
        val url = getImageUrl(doc)
        if (url != null) {
            try {
                val con = URL(url).openConnection()
                val stream = con.getInputStream()
                val bytes = stream.readBytes()
                stream.close()
                return bytes
            }catch (e: Exception){}
        }
    }catch (e: Exception){}
    return null
}

fun getImageNameByUrl(doc: Document): String? {
    try {
        var name = ""
        for (meta in doc.getElementsByTag("meta")) {
            if (meta.attr("name") == "description")
                name = meta.attr("content")
        }
        if (name == "") return null

        val index = name.indexOf(" - The #1 source for video game sprites on the internet!")
        return if (index != -1) name.substring(0, index)
        else name
    }catch (e: Exception){
        return null
    }
}

fun saveFile(name: String, bytes: ByteArray){
    val file = File(nameToFilename(name))
    file.createNewFile()
    file.writeBytes(bytes)
}

fun removeParameters(url: String): String{
    var url = url
    val questionMarkIndex = url.lastIndexOf("?")
    if(questionMarkIndex != -1)
        url = url.substring(0, questionMarkIndex)
    return url
}
fun getAbsoluteUrl(parent: String, child: String): String = parent + child

private fun nameToFilename(name: String): String {
    val s = name.filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '[' && it != ']' }
    var last = s.length
    if (last > 123) last = 123
    return s.substring(0, last) + ".png"
}

package com.example.opensonggoogletvviewer.parser

import com.example.opensonggoogletvviewer.model.CurrentSlide
import com.example.opensonggoogletvviewer.model.SlideDisplayMode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object OpenSongSlideParser {

    fun parseCurrentSlide(xml: String): CurrentSlide {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inSlides = false
        var inSlide = false
        var currentTag: String? = null

        var title: String? = null
        var body: String? = null
        var displayMode = SlideDisplayMode.Normal

        while (true) {
            when (parser.eventType) {
                XmlPullParser.START_DOCUMENT -> Unit

                XmlPullParser.START_TAG -> {
                    currentTag = parser.name

                    when (parser.name) {
                        "slides" -> inSlides = true
                        "slide" -> if (inSlides) inSlide = true
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text ?: ""
                    if (inSlides && inSlide) {
                        when (currentTag) {
                            "title" -> if (title == null) title = text.trim().takeIf { it.isNotEmpty() }
                            "body" -> if (body == null) body = text.trim().takeIf { it.isNotEmpty() }
                            "display" -> displayMode = when (text.trim().lowercase()) {
                                "black" -> SlideDisplayMode.Black
                                "blank" -> SlideDisplayMode.Blank
                                else -> SlideDisplayMode.Normal
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "slide" -> inSlide = false
                        "slides" -> inSlides = false
                    }
                    currentTag = null
                }

                XmlPullParser.END_DOCUMENT -> break
            }
            parser.next()
        }

        return CurrentSlide(title = title, body = body, displayMode = displayMode)
    }
}

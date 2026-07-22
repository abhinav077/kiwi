package com.abhinavsirohi.kiwi.feature.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinterestMetadataParserTest {
    @Test
    fun validator_acceptsPinterestHosts_andRejectsLookalikes() {
        assertEquals(
            "https://www.pinterest.com/pin/123/",
            PinterestUrlValidator.normalize("https://www.pinterest.com/pin/123/"),
        )
        assertEquals("https://pin.it/abc", PinterestUrlValidator.normalize("https://pin.it/abc"))
        assertNull(PinterestUrlValidator.normalize("https://pinterest.com.evil.example/pin/123"))
        assertNull(PinterestUrlValidator.normalize("javascript:alert(1)"))
    }

    @Test
    fun parser_extractsOnlyPinterestCdnVideoMetadata() {
        val html = """
            <html><head>
              <meta content="A quiet garden &amp; morning" property="og:title">
              <meta property="pinterestapp:creator_name" content="Garden Studio">
              <meta property="og:image" content="https://i.pinimg.com/736x/preview.jpg">
              <meta content="https://v1.pinimg.com/videos/video.mp4" property="og:video:secure_url">
              <meta property="og:video:type" content="video/mp4">
            </head></html>
        """.trimIndent()

        val result = PinterestMetadataParser.parse("https://www.pinterest.com/pin/123/", html)

        assertTrue(result is PinterestExtractionResult.Success)
        val media = (result as PinterestExtractionResult.Success).media
        assertEquals("A quiet garden & morning", media.title)
        assertEquals("Garden Studio", media.attribution)
        assertEquals("https://v1.pinimg.com/videos/video.mp4", media.mediaUrl)
    }

    @Test
    fun parser_rejectsMediaUrlOutsidePinterestCdn() {
        val html = "<meta property=\"og:video\" content=\"https://evil.example/video.mp4\">"

        val result = PinterestMetadataParser.parse("https://www.pinterest.com/pin/123/", html)

        assertTrue(result is PinterestExtractionResult.Failure)
    }

    @Test
    fun parser_extractsHighestQualityMp4FromStoryPinEmbeddedJson() {
        val html = """
            <meta property="og:title" content="Sunrise by the sea">
            <meta property="og:image" content="https://i.pinimg.com/736x/preview.jpg">
            <script>
              {"videoListMobile":{"vHLSV3MOBILE":{"url":"https:\/\/v1.pinimg.com\/videos\/clip.m3u8"}},
               "videoList":{"v540P":{"url":"https:\/\/v1.pinimg.com\/videos\/clip_540w.mp4"}},
               "videoList720P":{"v720P":{"url":"https:\u002F\u002Fv1.pinimg.com\u002Fvideos\u002Fclip_720w.mp4"}}}
            </script>
        """.trimIndent()

        val result = PinterestMetadataParser.parse("https://www.pinterest.com/pin/123/sent/", html)

        assertTrue(result is PinterestExtractionResult.Success)
        assertEquals(
            "https://v1.pinimg.com/videos/clip_720w.mp4",
            (result as PinterestExtractionResult.Success).media.mediaUrl,
        )
    }

    @Test
    fun parser_ignoresEmbeddedMp4OutsidePinterestCdn() {
        val html = """
            <script>{"url":"https://evil.example/clip_1080w.mp4"}</script>
            <script>{"url":"https://v1.pinimg.com/videos/clip_540w.mp4"}</script>
        """.trimIndent()

        val result = PinterestMetadataParser.parse("https://www.pinterest.com/pin/123/", html)

        assertEquals(
            "https://v1.pinimg.com/videos/clip_540w.mp4",
            (result as PinterestExtractionResult.Success).media.mediaUrl,
        )
    }
}

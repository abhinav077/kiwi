package com.abhinavsirohi.kiwi.data.remote

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoteErrorMapperTest {
    @Test
    fun networkErrors_areSafeAndKeepTechnicalDetailsPrivate() {
        val secretDetail = "request failed with key private-value"
        val error = RemoteErrorMapper.map(IOException(secretDetail))

        assertEquals(RemoteError.NetworkUnavailable, error)
        assertFalse(error.userMessage().contains(secretDetail))
        assertEquals("Kiwi will try again when you’re back online.", error.userMessage())
    }

    @Test
    fun unknownErrors_doNotExposeRawMessages() {
        val secretDetail = "server body contains private data"
        val error = RemoteErrorMapper.map(IllegalStateException(secretDetail))

        assertEquals(RemoteError.Unknown, error)
        assertFalse(error.userMessage().contains(secretDetail))
    }
}

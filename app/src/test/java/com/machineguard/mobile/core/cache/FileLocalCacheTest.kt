package com.machineguard.mobile.core.cache

import com.machineguard.mobile.core.FIXED_OFFSET_NOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileLocalCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var directory: File
    private lateinit var cache: FileLocalCache

    @Before
    fun setUp() {
        directory = File(folder.root, "api-cache")
        cache = FileLocalCache(directory)
    }

    @Test
    fun `an entry is returned with its payload and save time`() {
        cache.write("zones", """{"a":1}""", FIXED_OFFSET_NOW)

        val entry = cache.read("zones")!!

        assertEquals("""{"a":1}""", entry.payload)
        assertEquals(FIXED_OFFSET_NOW, entry.savedAt)
    }

    @Test
    fun `a payload with line breaks survives intact`() {
        val payload = "{\n  \"a\": 1,\n  \"b\": \"x\"\n}"
        cache.write("k", payload, FIXED_OFFSET_NOW)

        assertEquals(payload, cache.read("k")!!.payload)
    }

    @Test
    fun `writing the same key replaces the previous entry`() {
        cache.write("k", "old", FIXED_OFFSET_NOW)
        cache.write("k", "new", FIXED_OFFSET_NOW.plusMinutes(1))

        assertEquals("new", cache.read("k")!!.payload)
    }

    @Test
    fun `a missing entry is null`() {
        assertNull(cache.read("nothing"))
    }

    @Test
    fun `a damaged file is ignored and removed`() {
        cache.write("k", "payload", FIXED_OFFSET_NOW)
        directory.listFiles()!!.single { it.name.endsWith(".cache") }.writeText("basura sin formato")

        assertNull(cache.read("k"))
        assertEquals(0, directory.listFiles()!!.count { it.name.endsWith(".cache") })
    }

    @Test
    fun `entries can be removed by key prefix`() {
        cache.write("alerts?zone=&status=", "1", FIXED_OFFSET_NOW)
        cache.write("alert:7", "2", FIXED_OFFSET_NOW)
        cache.write("zones", "3", FIXED_OFFSET_NOW)

        cache.removeByPrefix("alert")

        assertNull(cache.read("alerts?zone=&status="))
        assertNull(cache.read("alert:7"))
        assertNotNull(cache.read("zones"))
    }

    @Test
    fun `clear removes everything`() {
        cache.write("a", "1", FIXED_OFFSET_NOW)
        cache.write("b", "2", FIXED_OFFSET_NOW)

        cache.clear()

        assertNull(cache.read("a"))
        assertNull(cache.read("b"))
    }
}

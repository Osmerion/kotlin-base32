/*
 * Copyright 2024-2025 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osmerion.kotlin.io.encoding

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.*

@OptIn(ExperimentalEncodingApi::class)
class Base32IoStreamTest {

    private fun testCoding(base32: Base32, text: String, encodedText: String) {
        val encodedBytes = ByteArray(encodedText.length) { encodedText[it].code.toByte() }
        val bytes = ByteArray(text.length) { text[it].code.toByte() }
        encodedBytes.inputStream().decodingWith(base32).use { inputStream ->
            assertEquals(text, inputStream.reader().readText())
        }
        encodedBytes.inputStream().decodingWith(base32).use { inputStream ->
            assertContentEquals(bytes, inputStream.readBytes())
        }
        ByteArrayOutputStream().let { outputStream ->
            outputStream.encodingWith(base32).use {
                it.write(bytes)
            }
            assertContentEquals(encodedBytes, outputStream.toByteArray())
        }
    }

    @Test
    fun base32() {
        fun testBase32(text: String, encodedText: String) {
            testCoding(Base32, text, encodedText)
        }

        testBase32("", "")
        testBase32("f", "MY======")
        testBase32("fo", "MZXQ====")
        testBase32("foo", "MZXW6===")
        testBase32("foob", "MZXW6YQ=")
        testBase32("fooba", "MZXW6YTB")
        testBase32("foobar", "MZXW6YTBOI======")
    }

    @Test
    fun base32Hex() {
        fun testBase32(text: String, encodedText: String) {
            testCoding(Base32.ExtendedHex, text, encodedText)
        }

        testBase32("", "")
        testBase32("f", "CO======")
        testBase32("fo", "CPNG====")
        testBase32("foo", "CPNMU===")
        testBase32("foob", "CPNMUOG=")
        testBase32("fooba", "CPNMUOJ1")
        testBase32("foobar", "CPNMUOJ1E8======")
    }

    @Test
    fun readDifferentOffsetAndLength() {
        val repeat = 10_000
        val symbols = "MZXW6YTB".repeat(repeat) + "MY======"
        val expected = "fooba".repeat(repeat) + "f"

        val bytes = ByteArray(expected.length)

        symbols.byteInputStream().decodingWith(Base32).use { input ->
            var read = 0
            repeat(8) {
                bytes[read++] = input.read().toByte()
            }

            var toRead = 1
            while (read < bytes.size) {
                val length = minOf(toRead, bytes.size - read)
                val result = input.read(bytes, read, length)

                assertEquals(length, result)

                read += result
                toRead += toRead * 10 / 7
            }

            assertEquals(-1, input.read(bytes))
            assertEquals(-1, input.read())
            assertEquals(expected, bytes.decodeToString())
        }
    }

    @Test
    fun readDifferentOffsetAndLengthHex() {
        val repeat = 10_000
        val symbols = "CPNMUOJ1".repeat(repeat) + "CO======"
        val expected = "fooba".repeat(repeat) + "f"

        val bytes = ByteArray(expected.length)

        symbols.byteInputStream().decodingWith(Base32.ExtendedHex).use { input ->
            var read = 0
            repeat(6) {
                bytes[read++] = input.read().toByte()
            }

            var toRead = 1
            while (read < bytes.size) {
                val length = minOf(toRead, bytes.size - read)
                val result = input.read(bytes, read, length)

                assertEquals(length, result)

                read += result
                toRead += toRead * 10 / 7
            }

            assertEquals(-1, input.read(bytes))
            assertEquals(-1, input.read())
            assertEquals(expected, bytes.decodeToString())
        }
    }

    @Test
    fun writeDifferentOffsetAndLength() {
        val repeat = 10_000
        val bytes = ("fooba".repeat(repeat) + "fo").encodeToByteArray()
        val expected = "MZXW6YTB".repeat(repeat) + "MZXQ===="

        val underlying = ByteArrayOutputStream()

        underlying.encodingWith(Base32).use { output ->
            var written = 0
            repeat(8) {
                output.write(bytes[written++].toInt())
            }
            var toWrite = 1
            while (written < bytes.size) {
                val length = minOf(toWrite, bytes.size - written)
                output.write(bytes, written, length)

                written += length
                toWrite += toWrite * 10 / 9
            }
        }

        assertEquals(expected, underlying.toString())
    }

    @Test
    fun writeDifferentOffsetAndLengthHex() {
        val repeat = 10_000
        val bytes = ("fooba".repeat(repeat) + "fo").encodeToByteArray()
        val expected = "CPNMUOJ1".repeat(repeat) + "CPNG===="

        val underlying = ByteArrayOutputStream()

        underlying.encodingWith(Base32.ExtendedHex).use { output ->
            var written = 0
            repeat(8) {
                output.write(bytes[written++].toInt())
            }
            var toWrite = 1
            while (written < bytes.size) {
                val length = minOf(toWrite, bytes.size - written)
                output.write(bytes, written, length)

                written += length
                toWrite += toWrite * 10 / 9
            }
        }

        assertEquals(expected, underlying.toString())
    }

    @Test
    fun inputStreamClosesUnderlying() {
        val underlying = object : InputStream() {
            var isClosed: Boolean = false

            override fun close() {
                isClosed = true
                super.close()
            }

            override fun read(): Int {
                return 0
            }
        }
        val wrapper = underlying.decodingWith(Base32)
        wrapper.close()
        assertTrue(underlying.isClosed)
    }

    @Test
    fun outputStreamClosesUnderlying() {
        val underlying = object : OutputStream() {
            var isClosed: Boolean = false

            override fun close() {
                isClosed = true
                super.close()
            }

            override fun write(b: Int) {
                // ignore
            }
        }
        val wrapper = underlying.encodingWith(Base32)
        wrapper.close()
        assertTrue(underlying.isClosed)
    }

    @Test
    fun correctPadding() {
        val inputStream = "MY======MY======".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())

            // in the wrapped IS the chars after the padding are not consumed
            assertContentEquals("MY======".toByteArray(), inputStream.readBytes())
        }

        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun correctPaddingHex() {
        val inputStream = "CO======CO======".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32.ExtendedHex)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())

            // in the wrapped IS the chars after the padding are not consumed
            assertContentEquals("CO======".toByteArray(), inputStream.readBytes())
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun illegalSymbol() {
        val inputStream = "MZ\u00FFXW6===".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32)

        wrapper.use {
            // one group of 8 symbols is read for decoding, that group includes illegal '\u00FF'
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun illegalSymbolHex() {
        val inputStream = "CP\u00FFNMU===".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32.ExtendedHex)

        wrapper.use {
            // one group of 8 symbols is read for decoding, that group includes illegal '\u00FF'
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun incorrectPadding() {
        val inputStream = "MZXW6YTBOI=MY======".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('b'.code, it.read())
            assertEquals('a'.code, it.read())

            // the second group is incorrectly padded
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun incorrectPaddingHex() {
        val inputStream = "CPNMUOJ1E8=CO======".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32.ExtendedHex)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('b'.code, it.read())
            assertEquals('a'.code, it.read())

            // the second group is incorrectly padded
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun withoutPadding() {
        for (paddingOption in Base32.PaddingOption.entries) {
            val configuredBase32 = Base32.withPadding(paddingOption)

            val inputStream = "MZXW6YTBOI".byteInputStream()
            val wrapper = inputStream.decodingWith(configuredBase32)

            wrapper.use {
                assertEquals('f'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('b'.code, it.read())
                assertEquals('a'.code, it.read())

                if (paddingOption == Base32.PaddingOption.PRESENT) {
                    assertFailsWith<IllegalArgumentException> { it.read() }
                } else {
                    assertEquals('r'.code, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                }
            }

            // closed
            assertFailsWith<IOException> {
                wrapper.read()
            }
        }
    }

    @Test
    fun withoutPaddingHex() {
        for (paddingOption in Base32.PaddingOption.entries) {
            val configuredBase32 = Base32.ExtendedHex.withPadding(paddingOption)

            val inputStream = "CPNMUOJ1E8".byteInputStream()
            val wrapper = inputStream.decodingWith(configuredBase32)

            wrapper.use {
                assertEquals('f'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('b'.code, it.read())
                assertEquals('a'.code, it.read())

                if (paddingOption == Base32.PaddingOption.PRESENT) {
                    assertFailsWith<IllegalArgumentException> { it.read() }
                } else {
                    assertEquals('r'.code, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                    assertEquals(-1, it.read())
                }
            }

            // closed
            assertFailsWith<IOException> {
                wrapper.read()
            }
        }
    }

    @Test
    fun nonZeroPadBits() {
        val inputStream = "MZXR====".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32)

        wrapper.use {
            assertFailsWith<IllegalArgumentException> { it.read() }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun nonZeroPadBitsHex() {
        val inputStream = "CPNH====".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32.ExtendedHex)

        wrapper.use {
            assertFailsWith<IllegalArgumentException> { it.read() }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun separatedPadSymbols() {
        val inputStream = "MZXW6YTBOI=====[,.|^&*@#]=".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('b'.code, it.read())
            assertEquals('a'.code, it.read())

            // the second group contains illegal symbols
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun separatedPadSymbolsHex() {
        val inputStream = "CPNMUOJ1E8=====[,.|^&*@#]=".byteInputStream()
        val wrapper = inputStream.decodingWith(Base32.ExtendedHex)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('b'.code, it.read())
            assertEquals('a'.code, it.read())
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

}
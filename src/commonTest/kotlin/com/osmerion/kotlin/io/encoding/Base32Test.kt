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

import com.osmerion.kotlin.io.encoding.Base32.PaddingOption.*
import kotlin.io.encoding.Base64
import kotlin.test.*

@OptIn(ExperimentalEncodingApi::class)
class Base32Test {

    private fun Base32.PaddingOption.isPresentOnEncode(): Boolean =
        this == PRESENT || this == PRESENT_OPTIONAL

    private fun Base32.PaddingOption.isOptionalOnDecode(): Boolean =
        this == PRESENT_OPTIONAL || this == ABSENT_OPTIONAL

    private fun Base32.PaddingOption.isAllowedOnDecode(): Boolean =
        this == PRESENT || isOptionalOnDecode()

    private fun testEncode(codec: Base32, bytes: ByteArray, expected: String) {
        assertEquals(expected, codec.encode(bytes))
        assertContentEquals(expected.encodeToByteArray(), codec.encodeToByteArray(bytes))
    }

    private fun testDecode(codec: Base32, symbols: String, expected: ByteArray) {
        assertContentEquals(expected, codec.decode(symbols))
        assertContentEquals(expected, codec.decode(symbols.encodeToByteArray()))
    }

    private fun testCoding(codec: Base32, bytes: ByteArray, symbols: String) {
        testEncode(codec, bytes, symbols)
        testDecode(codec, symbols, bytes)
    }

    private fun bytes(vararg values: Int): ByteArray {
        return ByteArray(values.size) { values[it].toByte() }
    }

    private val codecs = listOf(
        Base32 to "Basic",
        Base32.ExtendedHex to "ExtendedHex"
    )

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    @Test
    fun index() {
        val bytes = bytes(0b0000_0100, 0b0010_0000, 0b1100_0100, 0b0001_0100, 0b0110_0001, 0b1100_1000)

        for ((base32, scheme) in codecs) {
            val symbols = when (base32) {
                Base32 -> "AQQMIFDBZA======"
                Base32.ExtendedHex -> "0GGC8531P0======"
                else -> error("Unknown Base32 codec")
            }

            // encode
            let {
                testEncode(base32, bytes, symbols)
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.encode(bytes, startIndex = -1) }
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.encode(bytes, endIndex = bytes.size + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.encode(bytes, startIndex = bytes.size + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.encode(bytes, startIndex = 3, endIndex = 0) }

                assertEquals(symbols.substring(0, 8), base32.encode(bytes, endIndex = 5))
                assertEquals(symbols.substring(8), base32.encode(bytes, startIndex = 5))

                val destination = StringBuilder()
                base32.encodeToAppendable(bytes, destination, endIndex = 5)
                assertEquals(symbols.substring(0, 8), destination.toString())
                base32.encodeToAppendable(bytes, destination, startIndex = 5)
                assertEquals(symbols, destination.toString())
            }

            // encodeToByteArray
            let {
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.encodeToByteArray(bytes, startIndex = -1) }
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.encodeToByteArray(bytes, endIndex = bytes.size + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.encodeToByteArray(bytes, startIndex = bytes.size + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.encodeToByteArray(bytes, startIndex = 3, endIndex = 0) }

                assertContentEquals(symbols.encodeToByteArray(0, 8), base32.encodeToByteArray(bytes, endIndex = 5))
                assertContentEquals(symbols.encodeToByteArray(8), base32.encodeToByteArray(bytes, startIndex = 5))

                val destination = ByteArray(16)
                assertFailsWith<IndexOutOfBoundsException> { base32.encodeIntoByteArray(bytes, destination, destinationOffset = -1) }
                assertFailsWith<IndexOutOfBoundsException> { base32.encodeIntoByteArray(bytes, destination, destinationOffset = destination.size + 1) }
                assertFailsWith<IndexOutOfBoundsException> { base32.encodeIntoByteArray(bytes, destination, destinationOffset = 1) }

                assertTrue(destination.all { it == 0.toByte() })

                var length = base32.encodeIntoByteArray(bytes, destination, endIndex = 5)
                assertContentEquals(symbols.encodeToByteArray(0, 8), destination.copyOf(length))
                length += base32.encodeIntoByteArray(bytes, destination, destinationOffset = length, startIndex = 5)
                assertContentEquals(symbols.encodeToByteArray(), destination)
            }

            // decode(CharSequence)
            let {
                testDecode(base32, symbols, bytes)
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.decode(symbols, startIndex = -1) }
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.decode(symbols, endIndex = symbols.length + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.decode(symbols, startIndex = symbols.length + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.decode(symbols, startIndex = 8, endIndex = 0) }

                assertContentEquals(bytes.copyOfRange(0, 5), base32.decode(symbols, endIndex = 8))
                assertContentEquals(bytes.copyOfRange(5, bytes.size), base32.decode(symbols, startIndex = 8))

                val destination = ByteArray(6)
                assertFailsWith<IndexOutOfBoundsException> { base32.decodeIntoByteArray(symbols, destination, destinationOffset = -1) }
                assertFailsWith<IndexOutOfBoundsException> { base32.decodeIntoByteArray(symbols, destination, destinationOffset = destination.size + 1) }
                assertFailsWith<IndexOutOfBoundsException> { base32.decodeIntoByteArray(symbols, destination, destinationOffset = 1) }

                assertTrue(destination.all { it == 0.toByte() })

                var length = base32.decodeIntoByteArray(symbols, destination, endIndex = 8)
                assertContentEquals(bytes.copyOfRange(0, 5), destination.copyOf(length))
                length += base32.decodeIntoByteArray(symbols, destination, destinationOffset = length, startIndex = 8)
                assertContentEquals(bytes, destination)
            }

            // decode(ByteArray)
            let {
                val symbolBytes = symbols.encodeToByteArray()

                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.decode(symbolBytes, startIndex = -1) }
                assertFailsWith<IndexOutOfBoundsException>(scheme) { base32.decode(symbolBytes, endIndex = symbolBytes.size + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.decode(symbolBytes, startIndex = symbolBytes.size + 1) }
                assertFailsWith<IllegalArgumentException>(scheme) { base32.decode(symbolBytes, startIndex = 8, endIndex = 0) }

                assertContentEquals(bytes.copyOfRange(0, 5), base32.decode(symbolBytes, endIndex = 8))
                assertContentEquals(bytes.copyOfRange(5, bytes.size), base32.decode(symbolBytes, startIndex = 8))

                val destination = ByteArray(6)
                assertFailsWith<IndexOutOfBoundsException> { base32.decodeIntoByteArray(symbolBytes, destination, destinationOffset = -1) }
                assertFailsWith<IndexOutOfBoundsException> { base32.decodeIntoByteArray(symbolBytes, destination, destinationOffset = destination.size + 1) }
                assertFailsWith<IndexOutOfBoundsException> { base32.decodeIntoByteArray(symbolBytes, destination, destinationOffset = 1) }

                assertTrue(destination.all { it == 0.toByte() })

                var length = base32.decodeIntoByteArray(symbolBytes, destination, endIndex = 8)
                assertContentEquals(bytes.copyOfRange(0, 5), destination.copyOf(length))
                length += base32.decodeIntoByteArray(symbolBytes, destination, destinationOffset = length, startIndex = 8)
                assertContentEquals(bytes, destination)
            }

        }
    }

    @Test
    fun base32() {
        fun testEncode(codec: Base32, bytes: String, symbols: String) {
            if (codec.paddingOption.isPresentOnEncode()) {
                testEncode(codec, bytes.encodeToByteArray(), symbols)
            } else {
                testEncode(codec, bytes.encodeToByteArray(), symbols.trimEnd('='))
            }
        }

        fun testDecode(codec: Base32, symbols: String, bytes: String) {
            if (codec.paddingOption.isAllowedOnDecode()) {
                testDecode(codec, symbols, bytes.encodeToByteArray())
                if (codec.paddingOption.isOptionalOnDecode()) {
                    testDecode(codec, symbols.trimEnd('='), bytes.encodeToByteArray())
                }
            } else {
                testDecode(codec, symbols.trimEnd('='), bytes.encodeToByteArray())
            }
        }

        fun testCoding(codec: Base32, bytes: String, symbols: String) {
            testEncode(codec, bytes, symbols)
            testDecode(codec, symbols, bytes)
        }

        val codec = Base32.Default
        val scheme = "Base32"

        // By default, padding option is set to PRESENT
        assertSame(codec, codec.withPadding(Base32.PaddingOption.PRESENT))

        for (paddingOption in Base32.PaddingOption.entries) {
            val configuredCodec = codec.withPadding(paddingOption)
            testCoding(configuredCodec, "", "")
            testCoding(configuredCodec, "f", "MY======")
            testCoding(configuredCodec, "fo", "MZXQ====")
            testCoding(configuredCodec, "foo", "MZXW6===")
            testCoding(configuredCodec, "foob", "MZXW6YQ=")
            testCoding(configuredCodec, "fooba", "MZXW6YTB")
            testCoding(configuredCodec, "foobar", "MZXW6YTBOI======")

            val configuredScheme = "$scheme.withPadding(${paddingOption.name})"

            // at least two symbols are required
            val oneSymbol = listOf("M", "=", "@")
            for (symbol in oneSymbol) {
                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbol>") {
                    configuredCodec.decode(symbol)
                }.also { exception ->
                    assertContains(exception.message!!, "Input should have at least 2 symbols for Base32 decoding")
                }
            }

            // dangling single symbol at the end that does not have bits even for a byte
            val lastDanglingSymbol = listOf(
                "V=", "V==", "V===", "V====", "V=====", "V======", "V=======",
                "MZXW6YTBV", "MZXW6YTBV=", "MZXW6YTBV==", "MZXW6YTBV===", "MZXW6YTBV====", "MZXW6YTBV=====", "MZXW6YTBV======", "MZXW6YTBV======="
            )
            for (symbols in lastDanglingSymbol) {
                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                    configuredCodec.decode(symbols)
                }.also { exception ->
                    assertEquals("The last unit of input does not have enough bits", exception.message)
                }
            }

            // incorrect padding

            val redundantPadChar = listOf("NBSWY3DP=", "NBSWY3DPO5XXE3DE=")
            for (symbols in redundantPadChar) {
                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                    configuredCodec.decode(symbols)
                }.also { exception ->
                    assertEquals("Redundant pad character at index ${symbols.lastIndex}", exception.message)
                }
            }

            val missingOnePadChar = listOf("MZXW6==", "MZXW6==a", "MZXW6YTBOI=====", "MZXW6YTBOI=====\u0000")
            for (symbols in missingOnePadChar) {
                val errorMessage = if (paddingOption == Base32.PaddingOption.ABSENT)
                    "The padding option is set to ABSENT"
                else
                    "Missing pad characters at index"

                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                    configuredCodec.decode(symbols)
                }.also { exception ->
                    assertContains(exception.message!!, errorMessage)
                }
            }

            if (paddingOption == Base32.PaddingOption.ABSENT) {
                val withPadding = listOf("MY======", "MZXQ====", "MZXW6===", "MZXW6YQ=", "MZXW6YTBOI======")
                for (symbols in withPadding) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertContains(exception.message!!, "The padding option is set to ABSENT")
                    }
                }
            }

            if (paddingOption == Base32.PaddingOption.PRESENT) {
                val withoutPadding = listOf("MY", "MZXQ", "MZXW6", "MZXW6QY")
                for (symbols in withoutPadding) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertEquals(
                            "The padding option is set to PRESENT, but the input is not properly padded",
                            exception.message
                        )
                    }
                }
            }

            if (paddingOption.isAllowedOnDecode()) {
                val symbolAfterPadding = listOf("MZXW6===MZXW6===", "MZXW6====", "MZXW6YQ==", "MZXW6YQ=a")
                for (symbols in symbolAfterPadding) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertContains(exception.message!!, "prohibited after the pad character")
                    }
                }
            }

            val nonZeroPadBits = listOf("MZXR====", "MZXWF===", "MZXWEYJ=")
            for (symbols in nonZeroPadBits) {
                assertFailsWith<IllegalArgumentException>(("$configuredScheme <$symbols>")) {
                    codec.decode(symbols)
                }.also { exception ->
                    assertEquals("The pad bits must be zeros", exception.message)
                }
            }
        }
    }

    @Test
    fun base32hex() {
        fun testEncode(codec: Base32, bytes: String, symbols: String) {
            if (codec.paddingOption.isPresentOnEncode()) {
                testEncode(codec, bytes.encodeToByteArray(), symbols)
            } else {
                testEncode(codec, bytes.encodeToByteArray(), symbols.trimEnd('='))
            }
        }

        fun testDecode(codec: Base32, symbols: String, bytes: String) {
            if (codec.paddingOption.isAllowedOnDecode()) {
                testDecode(codec, symbols, bytes.encodeToByteArray())
                if (codec.paddingOption.isOptionalOnDecode()) {
                    testDecode(codec, symbols.trimEnd('='), bytes.encodeToByteArray())
                }
            } else {
                testDecode(codec, symbols.trimEnd('='), bytes.encodeToByteArray())
            }
        }

        fun testCoding(codec: Base32, bytes: String, symbols: String) {
            testEncode(codec, bytes, symbols)
            testDecode(codec, symbols, bytes)
        }

        val codec = Base32.ExtendedHex
        val scheme = "Base32"

        // By default, padding option is set to PRESENT
        assertSame(codec, codec.withPadding(Base32.PaddingOption.PRESENT))

        for (paddingOption in Base32.PaddingOption.entries) {
            val configuredCodec = codec.withPadding(paddingOption)
            testCoding(configuredCodec, "", "")
            testCoding(configuredCodec, "f", "CO======")
            testCoding(configuredCodec, "fo", "CPNG====")
            testCoding(configuredCodec, "foo", "CPNMU===")
            testCoding(configuredCodec, "foob", "CPNMUOG=")
            testCoding(configuredCodec, "fooba", "CPNMUOJ1")
            testCoding(configuredCodec, "foobar", "CPNMUOJ1E8======")

            val configuredScheme = "$scheme.withPadding(${paddingOption.name})"

            // at least two symbols are required
            val oneSymbol = listOf("M", "=", "@")
            for (symbol in oneSymbol) {
                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbol>") {
                    configuredCodec.decode(symbol)
                }.also { exception ->
                    assertContains(exception.message!!, "Input should have at least 2 symbols for Base32 decoding")
                }
            }

            // dangling single symbol at the end that does not have bits even for a byte
            val lastDanglingSymbol = listOf(
                "V=", "V==", "V===", "V====", "V=====", "V======", "V=======",
                "CPNMUOJ1V", "CPNMUOJ1V=", "CPNMUOJ1V==", "CPNMUOJ1V===", "CPNMUOJ1V====", "CPNMUOJ1V=====", "CPNMUOJ1V======", "CPNMUOJ1V======="
            )
            for (symbols in lastDanglingSymbol) {
                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                    configuredCodec.decode(symbols)
                }.also { exception ->
                    assertEquals("The last unit of input does not have enough bits", exception.message)
                }
            }

            // incorrect padding

            val redundantPadChar = listOf("D1IMOR3F=", "D1IMOR3FETNN4R34=")
            for (symbols in redundantPadChar) {
                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                    configuredCodec.decode(symbols)
                }.also { exception ->
                    assertEquals("Redundant pad character at index ${symbols.lastIndex}", exception.message)
                }
            }

            val missingOnePadChar = listOf("CPNMU==", "CPNMU==a", "CPNMUOJ1E8=====", "CPNMUOJ1E8=====\u0000")
            for (symbols in missingOnePadChar) {
                val errorMessage = if (paddingOption == Base32.PaddingOption.ABSENT)
                    "The padding option is set to ABSENT"
                else
                    "Missing pad characters at index"

                assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                    configuredCodec.decode(symbols)
                }.also { exception ->
                    assertContains(exception.message!!, errorMessage)
                }
            }

            if (paddingOption == Base32.PaddingOption.ABSENT) {
                val withPadding = listOf("CO======", "CPNG====", "CPNMU===", "CPNMUOG=", "CPNMUOJ1E8======")
                for (symbols in withPadding) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertContains(exception.message!!, "The padding option is set to ABSENT")
                    }
                }
            }

            if (paddingOption == Base32.PaddingOption.PRESENT) {
                val withoutPadding = listOf("CO", "CPNG", "CPNMU", "CPNMUOG")
                for (symbols in withoutPadding) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertEquals(
                            "The padding option is set to PRESENT, but the input is not properly padded",
                            exception.message
                        )
                    }
                }
            }

            if (paddingOption.isAllowedOnDecode()) {
                val symbolAfterPadding = listOf("CPNMU===CPNMU===", "CPNMU====", "CPNMUOG==", "CPNMUOG=a")
                for (symbols in symbolAfterPadding) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertContains(exception.message!!, "prohibited after the pad character")
                    }
                }
            }

            val nonZeroPadBits = listOf("CPNH====", "CPNM5===", "CPNM4O9=")
            for (symbols in nonZeroPadBits) {
                assertFailsWith<IllegalArgumentException>(("$configuredScheme <$symbols>")) {
                    codec.decode(symbols)
                }.also { exception ->
                    assertEquals("The pad bits must be zeros", exception.message)
                }
            }
        }
    }

    private val basicAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val extendedHexAlphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUV"

    private val alphabetBytes = ByteArray(20) { // Adjust size for 8 Base32 symbols per group of 5 bytes
        val symbol = it / 5 * 8
        when (it % 5) {
            0 -> (symbol shl 3) + ((symbol + 1) shr 2)
            1 -> ((symbol + 1) and 0x3 shl 6) + (symbol + 2 shl 1) + ((symbol + 3) shr 4)
            2 -> ((symbol + 3) and 0xF shl 4) + ((symbol + 4) shr 1)
            3 -> ((symbol + 4) and 0x1 shl 7) + (symbol + 5 shl 2) + ((symbol + 6) shr 3)
            else -> ((symbol + 6) and 0x7 shl 5) + (symbol + 7)
        }.toByte()
    }

    @Test
    fun basic() {
        testCoding(Base32, bytes(0b1111_1011, 0b1111_0000), "7PYA====")

        // all symbols from alphabet
        testCoding(Base32, alphabetBytes, basicAlphabet)

        // decode line separator
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZ\r\nXW6==") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZ\nXW6==") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZX\rW6==") }

        // decode illegal char
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZXW6(==") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZ[@]XW6===") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZ-XW6===") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZXW6=(%^)==") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZ\u00FFXW6===") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("\uFFFFMZXW6===") }
        assertFailsWith<IllegalArgumentException> { Base32.decode("MZXW6===\uD800\uDC00") }

        // long input
        val expected = "JBSWY3DPK5XXE3DE".repeat(76)
        testEncode(Base32, "HelloWorld".repeat(76).encodeToByteArray(), expected)
    }

    @Test
    fun extendedHex() {
        testCoding(Base32.ExtendedHex, bytes(0b1111_1011, 0b1111_0000), "VFO0====")

        // all symbols from alphabet
        testCoding(Base32.ExtendedHex, alphabetBytes, extendedHexAlphabet)

        // decode line separator
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CP\r\nNMU==") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CP\nNMU==") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CPN\rMU==") }

        // decode illegal char
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CPNMU(==") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CP[@]NMU===") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CP-NMU===") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CPNMU=(%^)==") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CP\u00FFNMU===") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("\uFFFFCPNMU===") }
        assertFailsWith<IllegalArgumentException> { Base32.ExtendedHex.decode("CPNMU===\uD800\uDC00") }

        // long input
        val expected = "91IMOR3FATNN4R34".repeat(76)
        testEncode(Base32.ExtendedHex, "HelloWorld".repeat(76).encodeToByteArray(), expected)
    }

    @Test
    fun encodeSize() {
        for ((codec, _) in codecs) {
            val paddingPresent = Base32.PaddingOption.entries.filter { it.isPresentOnEncode() }
            for (paddingOption in paddingPresent) {
                val configuredCodec = codec.withPadding(paddingOption)

                // One line in all schemes
                assertEquals(0, configuredCodec.encodeSize(0))

                assertEquals(8, configuredCodec.encodeSize(1))
                assertEquals(8, configuredCodec.encodeSize(2))
                assertEquals(8, configuredCodec.encodeSize(3))
                assertEquals(8, configuredCodec.encodeSize(4))
                assertEquals(8, configuredCodec.encodeSize(5))

                assertEquals(16, configuredCodec.encodeSize(6))
                assertEquals(16, configuredCodec.encodeSize(7))
                assertEquals(16, configuredCodec.encodeSize(8))
                assertEquals(16, configuredCodec.encodeSize(9))
                assertEquals(16, configuredCodec.encodeSize(10))

                assertEquals(24, configuredCodec.encodeSize(11))
                assertEquals(24, configuredCodec.encodeSize(12))
                assertEquals(24, configuredCodec.encodeSize(13))
                assertEquals(24, configuredCodec.encodeSize(14))
                assertEquals(24, configuredCodec.encodeSize(15))

                assertEquals(32, configuredCodec.encodeSize(16))

                // The maximum number of bytes that we can encode
                val limit = 1_342_177_275
                assertEquals(2_147_483_640, configuredCodec.encodeSize(limit))
                assertFailsWith<IllegalArgumentException> {
                    configuredCodec.encodeSize(limit + 1) // Int.MAX_VALUE + 1
                }.also { exception ->
                    assertEquals("Input is too big", exception.message)
                }

                assertFailsWith<IllegalArgumentException> {
                    configuredCodec.encodeSize(Int.MAX_VALUE)
                }.also { exception ->
                    assertEquals("Input is too big", exception.message)
                }
            }

            val paddingAbsent = Base32.PaddingOption.entries - paddingPresent.toSet()
            for (paddingOption in paddingAbsent) {
                val configuredCodec = codec.withPadding(paddingOption)

                // One line in all schemes
                assertEquals(0, configuredCodec.encodeSize(0))
                assertEquals(2, configuredCodec.encodeSize(1))
                assertEquals(4, configuredCodec.encodeSize(2))
                assertEquals(5, configuredCodec.encodeSize(3))
                assertEquals(7, configuredCodec.encodeSize(4))
                assertEquals(8, configuredCodec.encodeSize(5))
                assertEquals(10, configuredCodec.encodeSize(6))
                assertEquals(12, configuredCodec.encodeSize(7))
                assertEquals(13, configuredCodec.encodeSize(8))
                assertEquals(15, configuredCodec.encodeSize(9))
                assertEquals(16, configuredCodec.encodeSize(10))

                // The maximum number of bytes that we can encode
                val limit = 1_342_177_275
                assertEquals(2_147_483_640, configuredCodec.encodeSize(limit))
                assertEquals(2_147_483_642, configuredCodec.encodeSize(limit + 1))
                assertEquals(2_147_483_644, configuredCodec.encodeSize(limit + 2))
                assertEquals(2_147_483_645, configuredCodec.encodeSize(limit + 3))
                assertEquals(2_147_483_647, configuredCodec.encodeSize(limit + 4)) // Int.MAX_VALUE
                assertFailsWith<IllegalArgumentException> {
                    configuredCodec.encodeSize(limit + 8) // Int.MAX_VALUE + 1
                }.also { exception ->
                    assertEquals("Input is too big", exception.message)
                }

                assertFailsWith<IllegalArgumentException> {
                    configuredCodec.encodeSize(Int.MAX_VALUE)
                }.also { exception ->
                    assertEquals("Input is too big", exception.message)
                }
            }
        }
    }

    @Test
    fun decodeSize() {
        fun testDecodeSize(codec: Base32, symbols: String, expectedSize: Int) {
            assertEquals(expectedSize, codec.decodeSize(symbols.encodeToByteArray(), 0, symbols.length))
            assertEquals(
                expectedSize,
                codec.decodeSize(
                    if (symbols.isEmpty())
                        ByteArray(10)
                    else
                        ByteArray(symbols.length + 10) { symbols[(it - 5).coerceIn(0, symbols.lastIndex)].code.toByte() },
                    startIndex = 5,
                    endIndex = symbols.length + 5
                )
            )
        }

        for ((codec, _) in codecs) {
            assertFailsWith<IllegalArgumentException> {
                codec.decode(ByteArray(1), 0, 1)
            }.also { exception ->
                assertEquals("Input should have at least 2 symbols for Base32 decoding, startIndex: 0, endIndex: 1", exception.message)
            }
            assertFailsWith<IllegalArgumentException> {
                codec.decode(ByteArray(11), 5, 6)
            }.also { exception ->
                assertEquals("Input should have at least 2 symbols for Base32 decoding, startIndex: 5, endIndex: 6", exception.message)
            }

            testDecodeSize(codec, "", 0)

            testDecodeSize(codec, "MY======", 1)
            testDecodeSize(codec, "MY=====", 1)
            testDecodeSize(codec, "MY====", 1)
            testDecodeSize(codec, "MY===", 1)
            testDecodeSize(codec, "MY==", 1)
            testDecodeSize(codec, "MY=", 1)
            testDecodeSize(codec, "MY", 1)

            testDecodeSize(codec, "MZXQ====", 2)
            testDecodeSize(codec, "MZXQ===", 2)
            testDecodeSize(codec, "MZXQ==", 2)
            testDecodeSize(codec, "MZXQ=", 2)
            testDecodeSize(codec, "MZXQ", 2)

            testDecodeSize(codec, "MZXW6===", 3)
            testDecodeSize(codec, "MZXW6==", 3)
            testDecodeSize(codec, "MZXW6=", 3)
            testDecodeSize(codec, "MZXW6", 3)

            testDecodeSize(codec, "MZXW6YQ=", 4)
            testDecodeSize(codec, "MZXW6YQ", 4)

            testDecodeSize(codec, "MZXW6YTB", 5)

            testDecodeSize(codec, "MZXW6YTBOI======", 6)

            val longSymbols = "MZXW6YTB".repeat(76)
            testDecodeSize(codec, longSymbols, 5 * 76)
            testDecodeSize(codec, longSymbols + "MY======", 5 * 76 + 1)
            testDecodeSize(codec, longSymbols + "MY=====", 5 * 76 + 1)
            testDecodeSize(codec, longSymbols + "MY====", 5 * 76 + 1)
            testDecodeSize(codec, longSymbols + "MY===", 5 * 76 + 1)
            testDecodeSize(codec, longSymbols + "MY==", 5 * 76 + 1)
            testDecodeSize(codec, longSymbols + "MY=", 5 * 76 + 1)
            testDecodeSize(codec, longSymbols + "MY", 5 * 76 + 1)

            testDecodeSize(codec, "MY======" + longSymbols, 5 + 5 * 76)
            testDecodeSize(codec, "MY=====" + longSymbols, 4 + 5 * 76)
            testDecodeSize(codec, "MY====" + longSymbols, 3 + 5 * 76)
            testDecodeSize(codec, "M====" + longSymbols, 3 + 5 * 76)
            testDecodeSize(codec, "MY==" + longSymbols, 2 + 5 * 76)
            testDecodeSize(codec, "MY==" + longSymbols, 2 + 5 * 76)
            testDecodeSize(codec, "MY=" + longSymbols, 1 + 5 * 76)
            testDecodeSize(codec, "MY" + longSymbols, 1 + 5 * 76)
        }
    }

}
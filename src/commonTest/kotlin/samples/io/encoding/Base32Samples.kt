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
package samples.io.encoding

import com.osmerion.kotlin.io.encoding.Base32
import samples.*
import kotlin.test.*

class Base32Samples {

    @Sample
    fun encodeAndDecode() {
        val encoded = Base32.Default.encode("Hello, World!".encodeToByteArray())
        assertPrints(encoded, "JBSWY3DPFQQFO33SNRSCC===")

        val decoded = Base32.Default.decode(encoded)
        assertPrints(decoded.decodeToString(), "Hello, World!")
    }

    @Sample
    fun padding() {
        // "HelloW".length == 6, which is not multiple of 5;
        // base32-encoded data padded with 32 bits
        assertPrints(Base32.Default.encode("HelloW".encodeToByteArray()), "JBSWY3DPK4======")
        // "HelloWo".length == 7, which is not multiple of 5;
        // base32-encoded data padded with 24 bits
        assertPrints(Base32.Default.encode("HelloWo".encodeToByteArray()), "JBSWY3DPK5XQ====")
        // "HelloWor".length == 8, which is not multiple of 5;
        // base32-encoded data padded with 16 bits
        assertPrints(Base32.Default.encode("HelloWor".encodeToByteArray()), "JBSWY3DPK5XXE===")
        // "HelloWorl".length == 9, which is not multiple of 5;
        // base32-encoded data padded with 8 bits
        assertPrints(Base32.Default.encode("HelloWorl".encodeToByteArray()), "JBSWY3DPK5XXE3A=")
        // "HelloWorld".length == 10, which is the multiple of 5, so no padding is required
        assertPrints(Base32.Default.encode("HelloWorld".encodeToByteArray()), "JBSWY3DPK5XXE3DE")
    }

    @Sample
    fun encodingDifferences() {
        // Default encoding uses the alphabet 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
        assertPrints(Base32.Default.encode(byteArrayOf(-1, 0, -2, 0)), "74AP4AA=")
        // ExtendedHex encoding uses the alphabet '0123456789ABCDEFGHIJKLMNOPQRSTUV'
        assertPrints(Base32.ExtendedHex.encode(byteArrayOf(-1, 0, -2, 0)), "VS0FS00=")
    }

    @Sample
    fun paddingOptionSample() {
        val format = HexFormat {
            upperCase = true
            bytes.byteSeparator = " "
        }

        val bytes = byteArrayOf(0xDE.toByte(), 0x2D, 0x02, 0xC0.toByte())

        // The Base32.Default instance is configured with PaddingOption.PRESENT
        assertPrints(Base32.Default.encode(bytes), "3YWQFQA=")
        assertPrints(Base32.Default.decode("3YWQFQA=").toHexString(format), "DE 2D 02 C0")
        // PaddingOption.PRESENT requires the decode input to be properly padded
        assertFailsWith<IllegalArgumentException> {
            Base32.Default.decode("3YWQFQA")
        }

        // Create a new instance with PaddingOption.ABSENT that uses the Base32.Default alphabet
        val base32AbsentPadding = Base32.Default.withPadding(Base32.PaddingOption.ABSENT)

        assertPrints(base32AbsentPadding.encode(bytes), "3YWQFQA")
        assertPrints(base32AbsentPadding.decode("3YWQFQA").toHexString(format), "DE 2D 02 C0")
        // PaddingOption.ABSENT requires the decode input not to be padded
        assertFailsWith<IllegalArgumentException> {
            base32AbsentPadding.decode("3YWQFQA=")
        }

        // Create a new instance with PaddingOption.PRESENT_OPTIONAL that uses the Base32.Default alphabet
        val base32PresentOptionalPadding = Base32.Default.withPadding(Base32.PaddingOption.PRESENT_OPTIONAL)

        assertPrints(base32PresentOptionalPadding.encode(bytes), "3YWQFQA=")
        // PaddingOption.PRESENT_OPTIONAL allows both padded and unpadded decode inputs
        assertPrints(base32PresentOptionalPadding.decode("3YWQFQA=").toHexString(format), "DE 2D 02 C0")
        assertPrints(base32PresentOptionalPadding.decode("3YWQFQA").toHexString(format), "DE 2D 02 C0")
        // However, partially padded input is prohibited
        assertFailsWith<IllegalArgumentException> {
            base32PresentOptionalPadding.decode("3i0CwA=")
        }

        // Create a new instance with PaddingOption.ABSENT_OPTIONAL that uses the Base32.Default alphabet
        val base32AbsentOptionalPadding = Base32.Default.withPadding(Base32.PaddingOption.ABSENT_OPTIONAL)

        assertPrints(base32AbsentOptionalPadding.encode(bytes), "3YWQFQA")
        // PaddingOption.ABSENT_OPTIONAL allows both padded and unpadded decode inputs
        assertPrints(base32AbsentOptionalPadding.decode("3YWQFQA").toHexString(format), "DE 2D 02 C0")
        assertPrints(base32AbsentOptionalPadding.decode("3YWQFQA=").toHexString(format), "DE 2D 02 C0")
        // However, partially padded input is prohibited
        assertFailsWith<IllegalArgumentException> {
            base32AbsentOptionalPadding.decode("3i0CwA=")
        }
    }

    @Sample
    fun paddingOptionPresentSample() {
        val format = HexFormat { upperCase = true; bytes.byteSeparator = " " }
        val bytes = byteArrayOf(0xDE.toByte(), 0x2D, 0x02, 0xC0.toByte())

        // The predefined Base32 instances are configured with PaddingOption.PRESENT.
        // Hence, the same instance is returned.
        val isSameInstance = Base32.Default.withPadding(Base32.PaddingOption.PRESENT) === Base32.Default
        assertTrue(isSameInstance)

        // PaddingOption.PRESENT pads on encode, and requires padded input on decode
        assertPrints(Base32.Default.encode(bytes), "3YWQFQA=")
        assertPrints(Base32.Default.decode("3YWQFQA=").toHexString(format), "DE 2D 02 C0")
        // PaddingOption.PRESENT requires the decode input to be properly padded
        assertFailsWith<IllegalArgumentException> {
            Base32.Default.decode("3YWQFQA")
        }
    }

    @Sample
    fun paddingOptionAbsentSample() {
        val format = HexFormat { upperCase = true; bytes.byteSeparator = " " }
        val bytes = byteArrayOf(0xDE.toByte(), 0x2D, 0x02, 0xC0.toByte())

        val base32AbsentPadding = Base32.Default.withPadding(Base32.PaddingOption.ABSENT)

        // PaddingOption.ABSENT does not pad on encode, and requires unpadded input on decode
        assertPrints(base32AbsentPadding.encode(bytes), "3YWQFQA")
        assertPrints(base32AbsentPadding.decode("3YWQFQA").toHexString(format), "DE 2D 02 C0")
        // PaddingOption.ABSENT requires the decode input not to be padded
        assertFailsWith<IllegalArgumentException> {
            base32AbsentPadding.decode("3YWQFQA=")
        }
    }

    @Sample
    fun paddingOptionPresentOptionalSample() {
        val format = HexFormat { upperCase = true; bytes.byteSeparator = " " }
        val bytes = byteArrayOf(0xDE.toByte(), 0x2D, 0x02, 0xC0.toByte())

        val base32PresentOptionalPadding = Base32.Default.withPadding(Base32.PaddingOption.PRESENT_OPTIONAL)

        // PaddingOption.PRESENT_OPTIONAL pads on encode
        assertPrints(base32PresentOptionalPadding.encode(bytes), "3YWQFQA=")
        // It allows both padded and unpadded decode inputs
        assertPrints(base32PresentOptionalPadding.decode("3YWQFQA=").toHexString(format), "DE 2D 02 C0")
        assertPrints(base32PresentOptionalPadding.decode("3YWQFQA").toHexString(format), "DE 2D 02 C0")
        // However, partially padded input is prohibited
        assertFailsWith<IllegalArgumentException> {
            base32PresentOptionalPadding.decode("3i0CwA=")
        }
    }

    @Sample
    fun paddingOptionAbsentOptionalSample() {
        val format = HexFormat { upperCase = true; bytes.byteSeparator = " " }
        val bytes = byteArrayOf(0xDE.toByte(), 0x2D, 0x02, 0xC0.toByte())

        val base64AbsentOptionalPadding = Base32.Default.withPadding(Base32.PaddingOption.ABSENT_OPTIONAL)

        // PaddingOption.ABSENT_OPTIONAL does not pad on encode
        assertPrints(base64AbsentOptionalPadding.encode(bytes), "3YWQFQA")
        // It allows both padded and unpadded decode inputs
        assertPrints(base64AbsentOptionalPadding.decode("3YWQFQA").toHexString(format), "DE 2D 02 C0")
        assertPrints(base64AbsentOptionalPadding.decode("3YWQFQA=").toHexString(format), "DE 2D 02 C0")
        // However, partially padded input is prohibited
        assertFailsWith<IllegalArgumentException> {
            base64AbsentOptionalPadding.decode("MZXQ==")
        }
    }

    @Sample
    fun withPaddingSample() {
        // The predefined Base32.ExtendedHex instance uses PaddingOption.PRESENT.
        // Create a new instance with PaddingOption.ABSENT_OPTIONAL from the Base32.ExtendedHex instance.
        val base32ExtendedHexCustomPadding = Base32.ExtendedHex.withPadding(Base32.PaddingOption.ABSENT_OPTIONAL)

        // The new instance continues using the same ExtendedHex alphabet but omits padding when encoding
        assertPrints(base32ExtendedHexCustomPadding.encode(byteArrayOf(-1, 0, -2, 0)), "VS0FS00")
        // It allows decoding both padded and unpadded inputs
        assertPrints(base32ExtendedHexCustomPadding.decode("VS0FS00").contentToString(), "[-1, 0, -2, 0]")
        assertPrints(base32ExtendedHexCustomPadding.decode("VS0FS00=").contentToString(), "[-1, 0, -2, 0]")
    }

    @Sample
    fun defaultEncodingSample() {
        val encoded = Base32.Default.encode("Hello? :> ".encodeToByteArray())
        assertPrints(encoded, "JBSWY3DPH4QDUPRA")
    }

    @Sample
    fun hexEncodingSample() {
        val encoded = Base32.ExtendedHex.encode("Hello? :> ".encodeToByteArray())
        assertPrints(encoded, "91IMOR3F7SG3KFH0")
    }

    @Sample
    fun decodeFromByteArraySample() {
        // get a byte array filled with data, for instance, by reading it from a network
        val data = byteArrayOf(0x4E, 0x42, 0x53, 0x57, 0x59, 0x33, 0x44, 0x50)
        // decode data from the array
        assertTrue(Base32.decode(data).contentEquals("hello".encodeToByteArray()))

        val dataInTheMiddle = byteArrayOf(0x00, 0x00, 0x4E, 0x42, 0x53, 0x57, 0x59, 0x33, 0x44, 0x50, 0x00, 0x00)
        // decode base32-encoded data from the middle of a buffer
        val decoded = Base32.decode(dataInTheMiddle, startIndex = 2, endIndex = 10)
        assertTrue(decoded.contentEquals("hello".encodeToByteArray()))
    }

    @Sample
    fun decodeFromStringSample() {
        assertTrue(Base32.decode("74AP4AH5").contentEquals(byteArrayOf(-1, 0, -2, 0, -3)))

        val embeddedB32 = "Data is: \"74AP4AH5\""
        // find '"' indices and extract base64-encoded data in between
        val decoded = Base32.decode(
            embeddedB32,
            startIndex = embeddedB32.indexOf('"') + 1,
            endIndex = embeddedB32.lastIndexOf('"')
        )
        assertTrue(decoded.contentEquals(byteArrayOf(-1, 0, -2, 0, -3)))
    }

    @Sample
    fun decodeIntoByteArraySample() {
        val outputBuffer = ByteArray(1024)
        val inputChunks = listOf("INUHK3TLGE======", "KNSWG33OMRBWQ5LONM======", "MNUHK3TLEMZQ====")

        var bufferPosition = 0
        val chunkIterator = inputChunks.iterator()
        while (bufferPosition < outputBuffer.size && chunkIterator.hasNext()) {
            val chunk = chunkIterator.next()
            // fill buffer with data decoded from base64-encoded chunks
            // and increment current position by the number of decoded bytes
            bufferPosition += Base32.decodeIntoByteArray(
                chunk,
                destination = outputBuffer,
                destinationOffset = bufferPosition
            )
        }
        // consume outputBuffer
        assertPrints(outputBuffer.decodeToString(endIndex = bufferPosition), "Chunk1SecondChunkchunk#3")
    }

    @Sample
    fun decodeIntoByteArrayFromByteArraySample() {
        val outputBuffer = ByteArray(1024)
        // {data:\"MVXGG33EMVSA====\"}
        val data = byteArrayOf(123, 100, 97, 116, 97, 58, 34, 77, 86, 88, 71, 71, 51, 51, 69, 77, 86, 83, 65, 61, 61, 61, 61, 34, 125)
        val from = data.indexOf('"'.code.toByte()) + 1
        val until = data.lastIndexOf('"'.code.toByte())

        // decode subrange of input data into buffer and remember how many bytes were written
        val bytesWritten = Base32.decodeIntoByteArray(data, destination = outputBuffer, startIndex = from, endIndex = until)
        assertPrints(outputBuffer.decodeToString(endIndex = bytesWritten), "encoded")
    }

    @Sample
    fun encodeIntoByteArraySample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)
        val outputBuffer = ByteArray(1024)

        var bufferPosition = 0
        // encode data into buffer using Base32 encoding
        // and keep track of the number of bytes written
        bufferPosition += Base32.encodeIntoByteArray(data, outputBuffer)
        assertPrints(outputBuffer.decodeToString(endIndex = bufferPosition), "74AP4AH5")

        outputBuffer[bufferPosition++] = '|'.code.toByte()

        // encode data subrange to the buffer, writing it from the given offset
        bufferPosition += Base32.encodeIntoByteArray(
            data,
            destination = outputBuffer,
            destinationOffset = bufferPosition,
            startIndex = 1,
            endIndex = 3
        )
        assertPrints(outputBuffer.decodeToString(endIndex = bufferPosition), "74AP4AH5|AD7A====")
    }

    @Sample
    fun encodeToAppendableSample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)

        val encoded = buildString {
            append("{ \"data\": \"")
            Base32.encodeToAppendable(data, destination = this)
            append("\" }")
        }
        assertPrints(encoded, "{ \"data\": \"74AP4AH5\" }")

        val encodedFromSubRange = buildString {
            append("{ \"data\": \"")
            Base32.encodeToAppendable(data, destination = this, startIndex = 1, endIndex = 3)
            append("\" }")
        }
        assertPrints(encodedFromSubRange, "{ \"data\": \"AD7A====\" }")
    }

    @Sample
    fun encodeToByteArraySample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)

        val encoded = Base32.encodeToByteArray(data)
        assertTrue(encoded.contentEquals("74AP4AH5".encodeToByteArray()))

        val encodedFromSubRange = Base32.encodeToByteArray(data, startIndex = 1, endIndex = 3)
        assertTrue(encodedFromSubRange.contentEquals("AD7A====".encodeToByteArray()))
    }

    @Sample
    fun encodeToStringSample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)

        val encoded = Base32.encode(data)
        assertPrints(encoded, "74AP4AH5")

        val encodedFromSubRange = Base32.encode(data, startIndex = 1, endIndex = 3)
        assertPrints(encodedFromSubRange, "AD7A====")
    }

}
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

import kotlin.jvm.JvmOverloads
import kotlin.math.min

/**
 * Provides Base32 encoding and decoding functionality.
 * Base32 encoding, as defined by [RFC 4648](https://tools.ietf.org/html/rfc4648), transforms arbitrary binary data into
 * sequence of printable characters.
 *
 * For example, a sequence of bytes `0xC0 0xFF 0xEE` will be transformed to a string `"YD764==="` using a Base32
 * encoding defined by the [`RFC 4648`](https://www.rfc-editor.org/rfc/rfc4648). Decoding that string will result in the
 * original byte sequence.
 *
 * **Base32 is not an encryption scheme and should not be used when data needs to be secured or obfuscated.**
 *
 * Characters used by a particular Base32 scheme form an alphabet of 32 regular characters and an extra padding
 * character.
 *
 * The [default][Base32.Default] Base32 encoding scheme (usually referred to as just "base32") uses an alphabet
 * consistent of `'A'..'Z'` followed by `'2'..'7'`. [`RFC 4648 section 7`](https://www.rfc-editor.org/rfc/rfc4648#section-7)
 * defines the "base32hex" encoding scheme which uses an alternative alphabet of `'0'..'9'` followed by `'A'..'V'`.
 *
 * When decoding, a Base32 scheme usually accepts only characters from its alphabet, presence of any other characters is
 * treated as an error. That also implies that a Base32 scheme could not decode data encoded by another Base32 scheme if
 * their alphabets differ.
 *
 * In addition to 32 characters from the alphabet, Base32-encoded data may also contain one to seven padding characters
 * `'='` at its end. Base32 splits data that needs to be encoded into chunks of up to fives bytes each, which are then
 * transformed into a sequence of eight characters (meaning that every character encodes five bits of data). If the
 * number of bytes constituting input data is not a multiple of five (for instance, input consists of only three bytes),
 * the data will be padded by zero bits first and only then transformed into Base32-alphabet characters. If padding is
 * required, the resulting string is augmented with one or more '=' characters to make its length a multiple of eight
 * characters. Padding could range from zero to seven '=' characters, depending on the length of the input data. The
 * inclusion of padding characters in the resulting string depends on the [PaddingOption] set for the Base32 instance.
 *
 * This class is not supposed to be inherited or instantiated by calling its constructor. However, predefined instances
 * of this class are available for use. The companion object [Base32.Default] is the default instance of [Base32]. An
 * alternate implementation using the Base32 Extended Hex Alphabet is available as [Base32.ExtendedHex]. The padding
 * option for all predefined instances is set to [PaddingOption.PRESENT]. New instances with different padding options
 * can be created using the [withPadding] function.
 *
 * @sample  samples.io.encoding.Base32Samples.encodeAndDecode
 * @sample  samples.io.encoding.Base32Samples.encodingDifferences
 * @sample  samples.io.encoding.Base32Samples.padding
 *
 * @since   0.1.0
 */
@ExperimentalEncodingApi
public open class Base32 private constructor(
    internal val isHexExtended: Boolean,
    internal val paddingOption: PaddingOption,
) {

    /**
     * An enumeration of the possible padding options for Base32 encoding and decoding.
     *
     * Constants of this enum class can be passed to the [withPadding] function to create a new Base32 instance with the
     * specified padding option. Each padding option affects the encode and decode operations of the Base32 instance in
     * the following way:
     *
     * | PaddingOption                    | On encode    | On decode                |
     * |----------------------------------|--------------|--------------------------|
     * | [PaddingOption.PRESENT]          | Emit padding | Padding is required      |
     * | [PaddingOption.ABSENT]           | Omit padding | Padding must not present |
     * | [PaddingOption.PRESENT_OPTIONAL] | Emit padding | Padding is optional      |
     * | [PaddingOption.ABSENT_OPTIONAL]  | Omit padding | Padding is optional      |
     *
     * These options provide flexibility in handling the padding characters (`'='`) and enable compatibility with
     * various Base32 libraries and protocols.
     *
     * @sample  samples.io.encoding.Base32Samples.paddingOptionSample
     *
     * @since   0.1.0
     */
    public enum class PaddingOption {
        /**
         * Pad on encode, require padding on decode.
         *
         * When encoding, the result is padded with `'='` to reach an integral multiple of 8 symbols.
         * When decoding, correctly padded input is required. The padding character `'='` marks the end of the encoded
         * data, and subsequent symbols are prohibited.
         *
         * This represents the canonical form of Base64 encoding.
         *
         * @sample  samples.io.encoding.Base32Samples.paddingOptionPresentSample
         *
         * @since   0.1.0
         */
        PRESENT,

        /**
         * Do not pad on encode, prohibit padding on decode.
         *
         * When encoding, the result is not padded.
         * When decoding, the input must not contain any padding character.
         *
         * @sample  samples.io.encoding.Base32Samples.paddingOptionAbsentSample
         *
         * @since   0.1.0
         */
        ABSENT,

        /**
         * Pad on encode, allow optional padding on decode.
         *
         * When encoding, the result is padded with `'='` to reach an integral multiple of 8 symbols.
         * When decoding, the input may be either padded or unpadded. If the input contains a padding character, the
         * correct amount of padding character(s) must be present. The padding character `'='` marks the end of the
         * encoded data, and subsequent symbols are prohibited.
         *
         * @sample  samples.io.encoding.Base32Samples.paddingOptionPresentOptionalSample
         *
         * @since   0.1.0
         */
        PRESENT_OPTIONAL,

        /**
         * Do not pad on encode, allow optional padding on decode.
         *
         * When encoding, the result is not padded.
         * When decoding, the input may be either padded or unpadded. If the input contains a padding character, the
         * correct amount of padding character(s) must be present. The padding character `'='` marks the end of the
         * encoded data, and subsequent symbols are prohibited.
         *
         * @sample  samples.io.encoding.Base32Samples.paddingOptionAbsentOptionalSample
         *
         * @since   0.1.0
         */
        ABSENT_OPTIONAL
    }

    /**
     * Returns a new [Base32] instance that is equivalent to this instance but configured with the specified padding
     * [option].
     *
     * This method does not modify this instance. If the specified [option] is the same as the current padding option of
     * this instance, this instance itself is returned. Otherwise, a new instance is created using the same alphabet but
     * configured with the new padding option.
     *
     * @sample  samples.io.encoding.Base32Samples.withPaddingSample
     *
     * @since   0.1.0
     */
    public fun withPadding(option: PaddingOption): Base32 =
        if (paddingOption == option) this else Base32(isHexExtended, option)

    /**
     * Decodes symbols from the specified [source] array or its subrange.
     * Returns a [ByteArray] containing the resulting bytes.
     *
     * The requirement, prohibition, or optionality of padding in the input symbols is determined by the [PaddingOption]
     * set for this [Base32] instance.
     *
     * @param source        the array to decode symbols from.
     * @param startIndex    the beginning (inclusive) of the subrange to decode, 0 by default.
     * @param endIndex      the end (exclusive) of the subrange to decode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     * @throws IllegalArgumentException     when the symbols for decoding are not padded as required by the
     *                                      [PaddingOption] set for this [Base64] instance, or when there are extra
     *                                      symbols after the padding.
     *
     * @return  a [ByteArray] with the resulting bytes.
     *
     * @sample  samples.io.encoding.Base32Samples.decodeFromByteArraySample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun decode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray {
        checkSourceBounds(source.size, startIndex, endIndex)

        val decodeSize = decodeSize(source, startIndex, endIndex)
        val destination = ByteArray(decodeSize)

        val bytesWritten = decodeImpl(source, destination, 0, startIndex, endIndex)
        check(bytesWritten == destination.size)

        return destination
    }

    /**
     * Decodes symbols from the specified [source] char sequence or its substring.
     * Returns a [ByteArray] containing the resulting bytes.
     *
     * The requirement, prohibition, or optionality of padding in the input symbols is determined by the [PaddingOption]
     * set for this [Base32] instance.
     *
     * @param source        the char sequence to decode symbols from.
     * @param startIndex    the beginning (inclusive) of the substring to decode, 0 by default.
     * @param endIndex      the end (exclusive) of the substring to decode, length of the [source] by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     * @throws IllegalArgumentException     when the symbols for decoding are not padded as required by the
     *                                      [PaddingOption] set for this [Base32] instance, or when there are extra
     *                                      symbols after the padding.
     *
     * @return  a [ByteArray] with the resulting bytes.
     *
     * @sample  samples.io.encoding.Base32Samples.decodeFromStringSample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun decode(source: CharSequence, startIndex: Int = 0, endIndex: Int = source.length): ByteArray {
        val byteSource = platformCharsToBytes(source, startIndex, endIndex)
        return decode(byteSource)
    }

    /**
     * Decodes symbols from the specified [source] array or its subrange and writes resulting bytes into the
     * [destination] array.
     * Returns the number of bytes written.
     *
     * The requirement, prohibition, or optionality of padding in the input symbols is determined by the [PaddingOption]
     * set for this [Base32] instance.
     *
     * @param source            the array to decode symbols from.
     * @param destination       the array to write bytes into.
     * @param destinationOffset the starting index in the [destination] array to write bytes to, 0 by default.
     * @param startIndex        the beginning (inclusive) of the subrange to decode, 0 by default.
     * @param endIndex          the end (exclusive) of the subrange to decode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException    when the resulting bytes don't fit into the [destination] array starting at
     *                                      the specified [destinationOffset], or when that index is out of the
     *                                      [destination] array indices range.
     * @throws IllegalArgumentException     when the symbols for decoding are not padded as required by the
     *                                      [PaddingOption] set for this [Base32] instance, or when there are extra
     *                                      symbols after the padding.
     *
     * @return  the number of bytes written into [destination] array.
     *
     * @sample  samples.io.encoding.Base32Samples.decodeIntoByteArrayFromByteArraySample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun decodeIntoByteArray(source: ByteArray, destination: ByteArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = source.size): Int {
        checkSourceBounds(source.size, startIndex, endIndex)
        checkDestinationBounds(destination.size, destinationOffset, decodeSize(source, startIndex, endIndex))

        return decodeImpl(source, destination, destinationOffset, startIndex, endIndex)
    }

    /**
     * Decodes symbols from the specified [source] char sequence or its substring and writes resulting bytes into the
     * [destination] array.
     * Returns the number of bytes written.
     *
     * The requirement, prohibition, or optionality of padding in the input symbols
     * is determined by the [PaddingOption] set for this [Base32] instance.
     *
     * @param source the char sequence to decode symbols from.
     * @param destination the array to write bytes into.
     * @param destinationOffset the starting index in the [destination] array to write bytes to, 0 by default.
     * @param startIndex the beginning (inclusive) of the substring to decode, 0 by default.
     * @param endIndex the end (exclusive) of the substring to decode, length of the [source] by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException    when the resulting bytes don't fit into the [destination] array starting at
     *                                      the specified [destinationOffset], or when that index is out of the
     *                                      [destination] array indices range.
     * @throws IllegalArgumentException     when the symbols for decoding are not padded as required by the
     *                                      [PaddingOption] set for this [Base32] instance, or when there are extra
     *                                      symbols after the padding.
     *
     * @return  the number of bytes written into [destination] array.
     *
     * @sample  samples.io.encoding.Base32Samples.decodeIntoByteArraySample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun decodeIntoByteArray(source: CharSequence, destination: ByteArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = source.length): Int {
        val byteSource = platformCharsToBytes(source, startIndex, endIndex)
        return decodeIntoByteArray(byteSource, destination, destinationOffset)
    }

    private fun decodeBlock(source: ByteArray, startIndex: Int, endIndex: Int, destination: ByteArray, destinationOffset: Int): Int {
        var srcOffset = startIndex
        val alphabet = if (isHexExtended) base32HexDecodeMap else base32DecodeMap
        val blockLimit = srcOffset + ((endIndex - srcOffset) / 8) * 8
        var newDestOffset = destinationOffset

        while (srcOffset < blockLimit) {
            val b1 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b2 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b3 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b4 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b5 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b6 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b7 = alphabet[source[srcOffset++].toInt() and 0xFF]
            val b8 = alphabet[source[srcOffset++].toInt() and 0xFF]

            if ((b1 or b2 or b3 or b4 or b5 or b6 or b7 or b8) < 0) { // non base32 byte
                return newDestOffset - destinationOffset
            }

            val bits = (b1.toLong() shl 35 or (b2.toLong() shl 30) or (b3.toLong() shl 25) or (b4.toLong() shl 20)
                or (b5.toLong() shl 15) or (b6.toLong() shl 10) or (b7.toLong() shl 5) or b8.toLong())

            destination[newDestOffset++] = (bits shr 32).toByte()
            destination[newDestOffset++] = (bits shr 24).toByte()
            destination[newDestOffset++] = (bits shr 16).toByte()
            destination[newDestOffset++] = (bits shr 8).toByte()
            destination[newDestOffset++] = (bits).toByte()
        }

        return newDestOffset - destinationOffset
    }

    private fun decodeImpl(source: ByteArray, destination: ByteArray, destinationOffset: Int, startIndex: Int, endIndex: Int): Int {
        val alphabet = if (isHexExtended) base32HexDecodeMap else base32DecodeMap
        var offset = startIndex

        var dstOffset = destinationOffset
        var bits: Long = 0
        var shiftTo = 35

        val requiresPadding = ((startIndex - endIndex) % symbolsPerGroup) != 0
        var hasPadding = false

        while (offset < endIndex) {
            if (shiftTo == 35 && offset < endIndex - 8) {
                val dstLength: Int = this.decodeBlock(source, offset, endIndex, destination, dstOffset)
                val charsDecoded = (dstLength / 5) * 8

                offset += charsDecoded
                dstOffset += dstLength
            }

            if (offset >= endIndex) {
                break
            }

            var b: Int = source[offset++].toInt() and 0xFF
            if ((alphabet[b].also { b = it }) < 0) {
                if (b == -2) { // padding byte '='
                    if (shiftTo == 35 || shiftTo == 30 || shiftTo == 5 ||
                        shiftTo == 25 && (offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte()) ||
                        shiftTo == 20 && (offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte()) ||
                        shiftTo == 15 && (offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte()) ||
                        shiftTo == 10 && (offset == endIndex || source[offset++] != '='.code.toByte() || offset == endIndex || source[offset++] != '='.code.toByte())
                    ) {
                        throw IllegalArgumentException("Input byte array has wrong 5-byte ending unit")
                    }

                    hasPadding = true
                    break
                } else {
                    throw IllegalArgumentException("Illegal base32 character " + source[offset - 1].toString(radix = 16))
                }
            }

            bits = bits or (b.toLong() shl shiftTo)
            shiftTo -= 5

            if (shiftTo < 0) {
                destination[dstOffset++] = (bits shr 32).toByte()
                destination[dstOffset++] = (bits shr 24).toByte()
                destination[dstOffset++] = (bits shr 16).toByte()
                destination[dstOffset++] = (bits shr 8).toByte()
                destination[dstOffset++] = (bits).toByte()
                shiftTo = 35
                bits = 0
            }
        }

        if (shiftTo == 0 || shiftTo == 5) {
            destination[dstOffset++] = (bits shr 32).toByte()
            destination[dstOffset++] = (bits shr 24).toByte()
            destination[dstOffset++] = (bits shr 16).toByte()
            destination[dstOffset++] = (bits shr 8).toByte()
        } else if (shiftTo == 10) {
            destination[dstOffset++] = (bits shr 32).toByte()
            destination[dstOffset++] = (bits shr 24).toByte()
            destination[dstOffset++] = (bits shr 16).toByte()
        } else if (shiftTo == 15) {
            destination[dstOffset++] = (bits shr 32).toByte()
            destination[dstOffset++] = (bits shr 24).toByte()
        } else if ((shiftTo == 20) or (shiftTo == 25)) {
            destination[dstOffset++] = (bits shr 32).toByte()
        } else if (shiftTo == 30) {
            throw IllegalArgumentException("Last unit does not have enough valid bits")
        }

        if (requiresPadding && !hasPadding && paddingOption == PaddingOption.PRESENT)
            throw IllegalArgumentException("The padding option is set to PRESENT, but the input is not properly padded")
        else if (hasPadding && paddingOption == PaddingOption.ABSENT)
            throw IllegalArgumentException("The padding option is set to ABSENT, but the input is padded")

        require(offset >= endIndex) { "Input byte array has incorrect ending byte at $offset" }
        return dstOffset - destinationOffset
    }

    // `internal` for testing
    internal fun decodeSize(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        val symbols = endIndex - startIndex
        if (symbols == 0) return 0

        var paddings = 0
        for (i in 1..7) {
            if (source[endIndex - i] == padSymbol) {
                paddings++
            } else {
                break
            }
        }

        when (paddings) {
            3 -> paddings = 2
            4 -> paddings = 3
            6 -> paddings = 4
        }

        return ((symbols * bitsPerSymbol) / bitsPerByte) - paddings
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange.
     * Returns a string with the resulting symbols.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base32] instance.
     *
     * Use [encodeToByteArray] to get the output in [ByteArray] form.
     *
     * @param source        the array to encode bytes from.
     * @param startIndex    the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex      the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     *
     * @return  a string with the resulting symbols.
     *
     * @sample  samples.io.encoding.Base32Samples.encodeToStringSample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun encode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): String =
        platformEncodeToString(source, startIndex, endIndex)

    /**
     * Encodes bytes from the specified [source] array or its subrange and writes resulting symbols into the
     * [destination] array.
     * Returns the number of symbols written.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base32] instance.
     *
     * @param source            the array to encode bytes from.
     * @param destination       the array to write symbols into.
     * @param destinationOffset the starting index in the [destination] array to write symbols to, 0 by default.
     * @param startIndex        the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex          the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException    when the resulting symbols don't fit into the [destination] array starting
     *                                      at the specified [destinationOffset], or when that index is out of the
     *                                      [destination] array indices range.
     *
     * @return  the number of symbols written into [destination] array.
     *
     * @sample  samples.io.encoding.Base32Samples.encodeIntoByteArraySample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun encodeIntoByteArray(source: ByteArray, destination: ByteArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = source.size): Int =
        platformEncodeIntoByteArray(source, destination, destinationOffset, startIndex, endIndex)

    /**
     * Encodes bytes from the specified [source] array or its subrange and appends resulting symbols to the
     * [destination] appendable.
     * Returns the destination appendable.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base32] instance.
     *
     * @param source        the array to encode bytes from.
     * @param destination   the appendable to append symbols to.
     * @param startIndex    the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex      the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     *
     * @return  the destination appendable.
     *
     * @sample  samples.io.encoding.Base32Samples.encodeToAppendableSample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun <A : Appendable> encodeToAppendable(source: ByteArray, destination: A, startIndex: Int = 0, endIndex: Int = source.size): A {
        val stringResult = platformEncodeToString(source, startIndex, endIndex)
        destination.append(stringResult)
        return destination
    }

    /**
     * Encodes bytes from the specified [source] array or its subrange.
     * Returns a [ByteArray] containing the resulting symbols.
     *
     * Whether the encoding result is padded with `'='` depends on the [PaddingOption] set for this [Base32] instance.
     *
     * Each resulting symbol occupies one byte in the returned byte array.
     *
     * Use [encode] to get the output in string form.
     *
     * @param source        the array to encode bytes from.
     * @param startIndex    the beginning (inclusive) of the subrange to encode, 0 by default.
     * @param endIndex      the end (exclusive) of the subrange to encode, size of the [source] array by default.
     *
     * @throws IndexOutOfBoundsException    when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException     when `startIndex > endIndex`.
     *
     * @return  a [ByteArray] with the resulting symbols.
     *
     * @sample  samples.io.encoding.Base32Samples.encodeToByteArraySample
     *
     * @since   0.1.0
     */
    @JvmOverloads
    public fun encodeToByteArray(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray =
        platformEncodeToByteArray(source, startIndex, endIndex)

    internal fun encodeToByteArrayImpl(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        checkSourceBounds(source.size, startIndex, endIndex)

        val encodeSize = encodeSize(endIndex - startIndex)
        val destination = ByteArray(encodeSize)
        encodeIntoByteArrayImpl(source, destination, 0, startIndex, endIndex)

        return destination
    }

    internal fun encodeIntoByteArrayImpl(source: ByteArray, destination: ByteArray, destinationOffset: Int, startIndex: Int, endIndex: Int): Int {
        val alphabet = if (isHexExtended) base32HexEncodeMap else base32EncodeMap
        var srcOffset = startIndex
        var dstOffset = destinationOffset

        val slen: Int = ((endIndex - srcOffset) / 5) * 5
        val sl = srcOffset + slen

        while (srcOffset < sl) {
            val sl0 = min((srcOffset + slen).toDouble(), sl.toDouble()).toInt()

            while (srcOffset < sl) {
                val bits: Long = (source[srcOffset++].toInt() and 0xFF).toLong() shl 32 or
                    ((source[srcOffset++].toInt() and 0xFF).toLong() shl 24) or
                    ((source[srcOffset++].toInt() and 0xFF).toLong() shl 16) or
                    ((source[srcOffset++].toInt() and 0xFF).toLong() shl 8) or
                    (source[srcOffset++].toInt() and 0xFF).toLong()

                destination[dstOffset++] = alphabet[((bits ushr 35) and 31L).toInt()]
                destination[dstOffset++] = alphabet[((bits ushr 30) and 31L).toInt()]
                destination[dstOffset++] = alphabet[((bits ushr 25) and 31L).toInt()]
                destination[dstOffset++] = alphabet[((bits ushr 20) and 31L).toInt()]
                destination[dstOffset++] = alphabet[((bits ushr 15) and 31L).toInt()]
                destination[dstOffset++] = alphabet[((bits ushr 10) and 31L).toInt()]
                destination[dstOffset++] = alphabet[((bits ushr 5) and 31L).toInt()]
                destination[dstOffset++] = alphabet[(bits and 31L).toInt()]
            }

            dstOffset += (sl0 - srcOffset) / 5 * 8
            srcOffset = sl0
        }

//        if (srcOffset < endIndex) { // 1-4 leftover bytes
//            val b0: Int = source[srcOffset++].toInt() and 0xFF
//            destination[dstOffset++] = alphabet[b0 shr 3]
//
//            if (srcOffset == endIndex) {
//                destination[dstOffset++] = alphabet[(b0 shl 2) and 31]
//                repeat(6) { destination[dstOffset++] = padSymbol }
//            } else {
//                val b1: Int = source[srcOffset++].toInt() and 0xFF
//                destination[dstOffset++] = alphabet[(b0 shl 2) and 31 or (b1 shr 6)]
//                destination[dstOffset++] = alphabet[(b1 shr 1) and 31]
//
//                if (srcOffset == endIndex) {
//                    destination[dstOffset++] = alphabet[(b1 shl 4) and 31]
//                    repeat(4) { destination[dstOffset++] = padSymbol }
//                } else {
//                    val b2: Int = source[srcOffset++].toInt() and 0xFF
//                    destination[dstOffset++] = alphabet[(b1 shl 4) and 31 or (b2 shr 4)]
//
//                    if (srcOffset == endIndex) {
//                        destination[dstOffset++] = alphabet[(b2 shl 1) and 31]
//                        repeat(4) { destination[dstOffset++] = padSymbol }
//                    } else {
//                        val b3: Int = source[srcOffset].toInt() and 0xFF
//                        destination[dstOffset++] = alphabet[(b2 shl 1) and 31 or (b3 shr 7)]
//                        destination[dstOffset++] = alphabet[(b3 shr 2) and 31]
//                        destination[dstOffset++] = alphabet[(b3 shl 3) and 31]
//                        destination[dstOffset++] = padSymbol
//                    }
//                }
//            }
//        }

        if (srcOffset < endIndex) { // 1-4 leftover bytes
            // Load the first byte
            val b0: Int = source[srcOffset++].toInt() and 0xFF
            destination[dstOffset++] = alphabet[b0 shr 3]

            // Prepare a buffer to hold the next few bytes
            val nextBytes = ByteArray(4)
            var nextByteCount = 0

            while (srcOffset < endIndex && nextByteCount < 4) {
                nextBytes[nextByteCount++] = source[srcOffset++]
            }

            // Encode the remaining bytes
            val requiredPaddingSymbols = when (nextByteCount) {
                0 -> {
                    destination[dstOffset++] = alphabet[(b0 shl 2) and 31]
                    6
                }
                1 -> {
                    val b1 = nextBytes[0].toInt() and 0xFF
                    destination[dstOffset++] = alphabet[(b0 shl 2) and 31 or (b1 shr 6)]
                    destination[dstOffset++] = alphabet[(b1 shr 1) and 31]
                    destination[dstOffset++] = alphabet[(b1 shl 4) and 31]
                    4
                }
                2 -> {
                    val b1 = nextBytes[0].toInt() and 0xFF
                    val b2 = nextBytes[1].toInt() and 0xFF
                    destination[dstOffset++] = alphabet[(b0 shl 2) and 31 or (b1 shr 6)]
                    destination[dstOffset++] = alphabet[(b1 shr 1) and 31]
                    destination[dstOffset++] = alphabet[(b1 shl 4) and 31 or (b2 shr 4)]
                    destination[dstOffset++] = alphabet[(b2 shl 1) and 31]
                    3
                }
                3 -> {
                    val b1 = nextBytes[0].toInt() and 0xFF
                    val b2 = nextBytes[1].toInt() and 0xFF
                    val b3 = nextBytes[2].toInt() and 0xFF
                    destination[dstOffset++] = alphabet[(b0 shl 2) and 31 or (b1 shr 6)]
                    destination[dstOffset++] = alphabet[(b1 shr 1) and 31]
                    destination[dstOffset++] = alphabet[(b1 shl 4) and 31 or (b2 shr 4)]
                    destination[dstOffset++] = alphabet[(b2 shl 1) and 31 or (b3 shr 7)]
                    destination[dstOffset++] = alphabet[(b3 shr 2) and 31]
                    destination[dstOffset++] = alphabet[(b3 shl 3) and 31]
                    1
                }
                else -> throw IllegalStateException("Unexpected next byte count: $nextByteCount")
            }

            if (shouldPadOnEncode()) {
                repeat(requiredPaddingSymbols) { destination[dstOffset++] = padSymbol }
            }
        }

        return dstOffset - destinationOffset
    }

    internal fun encodeSize(sourceSize: Int): Int {
        require(sourceSize >= 0) { "Input size must be non-negative, but was $sourceSize" }

        val groups = sourceSize / bytesPerGroup
        val trailingBytes = sourceSize % bytesPerGroup
        var size = groups * symbolsPerGroup

        if (trailingBytes != 0) {
            size += if (shouldPadOnEncode()) symbolsPerGroup else (trailingBytes * bitsPerByte + 4) / bitsPerSymbol
        }

        if (size < 0) throw IllegalArgumentException("Input is too big")

        return size
    }

    private fun shouldPadOnEncode(): Boolean =
        paddingOption == PaddingOption.PRESENT || paddingOption == PaddingOption.PRESENT_OPTIONAL

    internal fun checkSourceBounds(sourceSize: Int, startIndex: Int, endIndex: Int) {
        if (startIndex < 0 || endIndex > sourceSize) throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $sourceSize")
        if (startIndex > endIndex) throw IllegalArgumentException("startIndex: $startIndex > endIndex: $endIndex")
    }

    private fun checkDestinationBounds(destinationSize: Int, destinationOffset: Int, capacityNeeded: Int) {
        if (destinationOffset < 0 || destinationOffset > destinationSize) throw IndexOutOfBoundsException("destination offset: $destinationOffset, destination size: $destinationSize")

        val destinationEndIndex = destinationOffset + capacityNeeded
        if (destinationEndIndex < 0 || destinationEndIndex > destinationSize) {
            throw IndexOutOfBoundsException(
                "The destination array does not have enough capacity, " +
                    "destination offset: $destinationOffset, destination size: $destinationSize, capacity needed: $capacityNeeded"
            )
        }
    }

    internal fun charsToBytesImpl(source: CharSequence, startIndex: Int, endIndex: Int): ByteArray {
        checkSourceBounds(source.length, startIndex, endIndex)

        val byteArray = ByteArray(endIndex - startIndex)
        var length = 0

        for (index in startIndex until endIndex) {
            val symbol = source[index].code
            if (symbol <= 0xFF) {
                byteArray[length++] = symbol.toByte()
            } else {
                // the replacement byte must be an illegal symbol
                // so that mime skips it and basic throws with correct index
                byteArray[length++] = 0x3F
            }
        }

        return byteArray
    }

    internal fun bytesToStringImpl(source: ByteArray): String {
        val stringBuilder = StringBuilder(source.size)

        for (byte in source) {
            stringBuilder.append(byte.toInt().toChar())
        }

        return stringBuilder.toString()
    }

    /**
     * The "base32" encoding specified by [`RFC 4648 section 6`](https://www.rfc-editor.org/rfc/rfc4648#section-6),
     * Base 32 Encoding.
     *
     * Uses "The Base 32 Alphabet" as specified in Table 3 of RFC 4648 for encoding and decoding, consisting of
     * `'A'..'Z'`, and `'2'..'7'` characters.
     *
     * This instance is configured with the padding option set to [PaddingOption.PRESENT].
     * Use the [withPadding] function to create a new instance with a different padding option if necessary.
     *
     * Encode operation does not add any line separator character.
     * Decode operation throws if it encounters a character outside the base64 alphabet.
     *
     * @sample  samples.io.encoding.Base32Samples.defaultEncodingSample
     *
     * @since   0.1.0
     */
    public companion object Default : Base32(isHexExtended = false, PaddingOption.PRESENT) {

        private const val bitsPerByte: Int = 8
        private const val bitsPerSymbol: Int = 5

        internal const val bytesPerGroup: Int = 5
        internal const val symbolsPerGroup: Int = 8

        internal const val padSymbol: Byte = 61 // '='

        /**
         * The "base32hex" encoding specified by [`RFC 4648 section 7`](https://www.rfc-editor.org/rfc/rfc4648#section-7),
         * Base 32 Encoding with Hex Alphabet.
         *
         * Uses "The URL and Filename safe Base 64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding,
         * consisting of `'A'..'Z'`, `'a'..'z'`, `'-'` and `'_'` characters.
         *
         * This instance is configured with the padding option set to [PaddingOption.PRESENT].
         * Use the [withPadding] function to create a new instance with a different padding option if necessary.
         *
         * Encode operation does not add any line separator character.
         * Decode operation throws if it encounters a character outside the base64url alphabet.
         *
         * @sample  samples.io.encoding.Base32Samples.hexEncodingSample
         *
         * @since   0.1.0
         */
        public val ExtendedHex: Base32 = Base32(isHexExtended = true, PaddingOption.PRESENT)

    }

}

// "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
private val base32EncodeMap = byteArrayOf(
    65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  /* 0 - 15 */
    81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  50,  51,  52,  53,  54,  55   /* 16 - 31 */
)

@ExperimentalEncodingApi
private val base32DecodeMap = IntArray(256).apply {
    this.fill(-1)
    this[Base32.padSymbol.toInt()] = -2
    base32EncodeMap.forEachIndexed { index, symbol ->
        this[symbol.toInt()] = index
    }
}

// "0123456789ABCDEFGHIJKLMNOPQRSTUV"
private val base32HexEncodeMap = byteArrayOf(
    48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  65,  66,  67,  68,  69,  70,  /* 0 - 15 */
    71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  81,  82,  83,  84,  85,  86   /* 16 - 31 */
)

@ExperimentalEncodingApi
private val base32HexDecodeMap = IntArray(256).apply {
    this.fill(-1)
    this[Base32.padSymbol.toInt()] = -2
    base32HexEncodeMap.forEachIndexed { index, symbol ->
        this[symbol.toInt()] = index
    }
}

@ExperimentalEncodingApi
internal expect fun Base32.platformCharsToBytes(
    source: CharSequence,
    startIndex: Int,
    endIndex: Int,
): ByteArray

@ExperimentalEncodingApi
internal expect fun Base32.platformEncodeToString(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int,
): String

@ExperimentalEncodingApi
internal expect fun Base32.platformEncodeIntoByteArray(
    source: ByteArray,
    destination: ByteArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int,
): Int

@ExperimentalEncodingApi
internal expect fun Base32.platformEncodeToByteArray(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int,
): ByteArray
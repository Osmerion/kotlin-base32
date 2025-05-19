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

internal actual fun Base32.platformCharsToBytes(
    source: CharSequence,
    startIndex: Int,
    endIndex: Int
): ByteArray {
    return if (source is String) {
        checkSourceBounds(source.length, startIndex, endIndex)
        // up to 10x faster than the Common implementation
        source.substring(startIndex, endIndex).toByteArray(Charsets.ISO_8859_1)
    } else {
        charsToBytesImpl(source, startIndex, endIndex)
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Base32.platformEncodeToString(source: ByteArray, startIndex: Int, endIndex: Int): String {
    val byteResult = encodeToByteArrayImpl(source, startIndex, endIndex)
    return String(byteResult, Charsets.ISO_8859_1)
}

internal actual fun Base32.platformEncodeIntoByteArray(
    source: ByteArray,
    destination: ByteArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
): Int =
    encodeIntoByteArrayImpl(source, destination, destinationOffset, startIndex, endIndex)

internal actual fun Base32.platformEncodeToByteArray(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int
): ByteArray =
    encodeToByteArrayImpl(source, startIndex, endIndex)
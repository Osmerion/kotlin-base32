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
import com.osmerion.kotlin.io.encoding.ExperimentalEncodingApi
import com.osmerion.kotlin.io.encoding.decodingWith
import com.osmerion.kotlin.io.encoding.encodingWith
import samples.Sample
import samples.assertPrints
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalEncodingApi::class)
class Base32StreamsSample {

    @Sample
    fun base32InputStream() {
        ByteArrayInputStream("K5SWYY3PNVSSAQTBONSTGMRB".toByteArray()).decodingWith(Base32.Default).use {
            assertPrints(it.readBytes().decodeToString(), "Welcome Base32!")
        }

        ByteArrayInputStream("KBQWIOQ=... and everything else".toByteArray()).use { input ->
            input.decodingWith(Base32.Default).also {
                // Reads only Base32-encoded part, including padding
                assertPrints(it.readBytes().decodeToString(), "Pad:")
            }
            // The original stream will only contain remaining bytes
            assertPrints(input.readBytes().decodeToString(), "... and everything else")
        }
    }

    @Sample
    fun base32OutputStream() {
        ByteArrayOutputStream().also { out ->
            out.encodingWith(Base32.Default).use {
                it.write("Hello World!!".encodeToByteArray())
            }
            assertPrints(out.toString(), "JBSWY3DPEBLW64TMMQQSC===")
        }
    }

}
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

import kotlin.annotation.AnnotationTarget.*

/*
 * We use a custom marker annotation instead of `@kotlin.ExperimentalUnsignedTypes` for now to explicitly communicate
 * that this API is not part of the Kotlin standard library (yet?).
 */

/**
 * This annotation marks the experimental API for encoding and decoding between binary data and printable ASCII
 * character sequences.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary
 * incompatible with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalEncodingApi` must be accepted either by annotating that usage
 * with the [OptIn] annotation, e.g. `@OptIn(ExperimentalEncodingApi::class)`, or by using the compiler argument
 * `-opt-in=com.osmerion.kotlin.io.encoding.ExperimentalEncodingApi`.
 *
 * @since   0.1.0
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
@MustBeDocumented
public annotation class ExperimentalEncodingApi
### 1.0.0

_Released 2025 Jun 29_

#### Improvements

- Added extensions for working with Java streams.
    - The `decodingWith` and `encodingWith` functions are now available on `InputStream` and `OutputStream` and mirror
      the standard library's API for Base64.
- The `Base32` API is now stable and no longer marked as experimental.

#### Breaking Changes

- The library now requires Kotlin 2.2 (up from 2.1).
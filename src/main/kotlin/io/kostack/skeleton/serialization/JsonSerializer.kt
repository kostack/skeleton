package io.kostack.skeleton.serialization

import kotlinx.serialization.json.Json

object JsonSerializer {
  inline fun <reified T> encode(obj: T): String = jsonSerializer.encodeToString<T>(obj)

  inline fun <reified T> decode(json: String): T = jsonSerializer.decodeFromString<T>(json)

  val jsonSerializer =
    Json {
      isLenient = true
      ignoreUnknownKeys = true
      encodeDefaults = true
    }
}

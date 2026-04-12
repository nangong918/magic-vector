package com.vectordemo.utils.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/** JSON [userId] as decimal string (preferred) or number; always as [String] for Gson fields. */
class WireUserIdJsonDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): String? {
        if (json.isJsonNull) return null
        val p = json.asJsonPrimitive
        return when {
            p.isString -> p.asString
            p.isNumber -> p.asLong.toString()
            else -> null
        }
    }
}

package com.vectordemo.domain.dto.http.request

import io.ktor.client.request.forms.FormBuilder
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append

fun FormBuilder.appendPayload(part: MultipartPartPayload, formFieldName: String = part.fieldName) {
    append(
        key = formFieldName,
        value = part.bytes,
        headers = Headers.build {
            append(
                HttpHeaders.ContentDisposition,
                ContentDisposition.File
                    .withParameter(ContentDisposition.Parameters.Name, formFieldName)
                    .withParameter(ContentDisposition.Parameters.FileName, part.fileName)
                    .toString(),
            )
            append(HttpHeaders.ContentType, part.mimeType)
        },
    )
}

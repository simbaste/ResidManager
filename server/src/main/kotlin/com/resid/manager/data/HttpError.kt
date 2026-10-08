package com.resid.manager.data

import io.ktor.http.HttpStatusCode

class HttpError(
    val code: HttpStatusCode,
    override val message: String,
): Exception(message)

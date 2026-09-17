package kz.kimep.mobile.data

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiException(val status: Int, message: String) : Exception(message)

fun Throwable.friendlyMessage(): String = when (this) {
    is ApiException -> when (status) {
        400, 401 -> "Incorrect Student ID or password"
        403 -> "Access denied"
        404 -> "Requested data was not found"
        in 500..599 -> "KIMEP server error ($status). Try again later"
        else -> "Request failed ($status)"
    }
    is UnknownHostException -> "No internet connection"
    is ConnectException -> "Cannot reach the KIMEP server"
    is SocketTimeoutException -> "The server took too long to respond"
    is IOException -> "Network error. Check your connection"
    else -> message ?: "Something went wrong"
}

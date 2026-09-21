package com.example.catslist.data.error

import android.database.sqlite.SQLiteException
import android.util.Log
import com.example.catslist.domain.NetworkMonitor
import com.example.catslist.domain.model.AppError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Turns whatever was thrown into an [AppError]. The only place in the app that knows what a
 * `SocketTimeoutException` or an HTTP 429 means (ADR-0028).
 */
@Singleton
class ErrorMapper @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) {

    /**
     * `suspend`, because separating [AppError.NoConnection] from [AppError.Unreachable] takes
     * asking [NetworkMonitor] — and asking it *now*, when the failure happened, rather than
     * before the request when the answer would only have been a guess.
     */
    suspend fun map(error: Throwable): AppError {
        val mapped = when (error) {
            is HttpException -> error.code().toAppError()
            is SocketTimeoutException -> AppError.Timeout
            is UnknownHostException -> transportFailure()
            is SerializationException -> AppError.Malformed
            is SQLiteException -> AppError.Storage
            // After the specific ones: SocketTimeoutException and UnknownHostException are
            // both IOException, and a `when` takes the first branch that matches.
            is IOException -> transportFailure()
            else -> AppError.Unknown
        }
        // The one place the throwable itself is still available, so the one place worth
        // logging it. Downstream carries the classification only.
        Log.w(TAG, "Mapped ${error::class.simpleName} to $mapped", error)
        return mapped
    }

    /**
     * The request never reached the server. Which of the two that is depends entirely on the
     * device: offline is the more useful thing to say, and it is only true if it is true.
     */
    private suspend fun transportFailure(): AppError =
        if (networkMonitor.isOnline.first()) AppError.Unreachable else AppError.NoConnection

    private fun Int.toAppError(): AppError = when {
        this == HTTP_TOO_MANY_REQUESTS -> AppError.RateLimited
        this in SERVER_ERRORS -> AppError.Server(this)
        this in CLIENT_ERRORS -> AppError.Client(this)
        else -> AppError.Unknown
    }

    private companion object {
        const val TAG = "ErrorMapper"
        const val HTTP_TOO_MANY_REQUESTS = 429
        val CLIENT_ERRORS = 400..499
        val SERVER_ERRORS = 500..599
    }
}

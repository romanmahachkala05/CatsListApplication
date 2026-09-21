package com.example.catslist.data.error

import android.database.sqlite.SQLiteException
import com.example.catslist.domain.model.AppError
import com.example.catslist.testing.FakeNetworkMonitor
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErrorMapperTest {

    private val networkMonitor = FakeNetworkMonitor()
    private val mapper = ErrorMapper(networkMonitor)

    @Test
    fun `a timeout is a timeout whether or not the device is online`() = runTest {
        // The connection was there — it was the request that ran out of time, so connectivity
        // has nothing to add.
        networkMonitor.setOnline(false)

        assertThat(mapper.map(SocketTimeoutException("timeout"))).isEqualTo(AppError.Timeout)
    }

    @Test
    fun `an unresolvable host while offline is NoConnection`() = runTest {
        networkMonitor.setOnline(false)

        assertThat(mapper.map(UnknownHostException("api.thecatapi.com"))).isEqualTo(AppError.NoConnection)
    }

    @Test
    fun `the same unresolvable host while online is Unreachable`() = runTest {
        // The distinction the NetworkMonitor exists for: identical exception, and the only
        // thing that separates "you are offline" from "their DNS is down" is asking.
        networkMonitor.setOnline(true)

        assertThat(mapper.map(UnknownHostException("api.thecatapi.com"))).isEqualTo(AppError.Unreachable)
    }

    @Test
    fun `a refused connection is a transport failure like any other`() = runTest {
        // ConnectException is neither of the two named subclasses, so this is the general
        // IOException branch — it must still consult connectivity rather than fall to Unknown.
        networkMonitor.setOnline(true)

        assertThat(mapper.map(ConnectException("refused"))).isEqualTo(AppError.Unreachable)
    }

    @Test
    fun `a plain IOException while offline is still NoConnection`() = runTest {
        networkMonitor.setOnline(false)

        assertThat(mapper.map(IOException("socket closed"))).isEqualTo(AppError.NoConnection)
    }

    @Test
    fun `429 is its own case, not a generic client error`() = runTest {
        // TheCatAPI rate-limits anonymous callers, so this is the 4xx users actually hit —
        // and the only one where waiting is the right advice.
        assertThat(mapper.map(httpException(429))).isEqualTo(AppError.RateLimited)
    }

    @Test
    fun `a 5xx keeps its code`() = runTest {
        assertThat(mapper.map(httpException(503))).isEqualTo(AppError.Server(503))
    }

    @Test
    fun `a 4xx other than 429 keeps its code`() = runTest {
        assertThat(mapper.map(httpException(404))).isEqualTo(AppError.Client(404))
    }

    @Test
    fun `an HTTP code outside 4xx and 5xx is not forced into either`() = runTest {
        // Retrofit only throws HttpException for non-2xx, but a 3xx that OkHttp did not follow
        // reaches here, and calling it a client error would be a guess.
        assertThat(mapper.map(httpException(301))).isEqualTo(AppError.Unknown)
    }

    @Test
    fun `a parse failure is Malformed, not a network problem`() = runTest {
        // A 200 whose body no longer matches the wire model. Telling the user to check their
        // connection would send them after the wrong thing entirely.
        assertThat(mapper.map(SerializationException("Unexpected JSON token"))).isEqualTo(AppError.Malformed)
    }

    @Test
    fun `a database failure is Storage`() = runTest {
        assertThat(mapper.map(SQLiteException("disk I/O error"))).isEqualTo(AppError.Storage)
    }

    @Test
    fun `anything unrecognised is Unknown`() = runTest {
        assertThat(mapper.map(IllegalStateException("something else"))).isEqualTo(AppError.Unknown)
    }

    /**
     * Built over a raw OkHttp response rather than `Response.error(code, body)`, which rejects
     * anything below 400 — and the mapper's fallthrough branch is exactly the codes below 400.
     */
    private fun httpException(code: Int): HttpException {
        val raw = OkHttpResponse.Builder()
            .request(Request.Builder().url("https://api.thecatapi.com/v1/images/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .build()
        return HttpException(Response.error<Unit>("".toResponseBody("application/json".toMediaType()), raw))
    }
}

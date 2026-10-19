package com.myarulin.movies_test.data.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OmdbApiIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OmdbApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OmdbApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search response is parsed and request contains pagination`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "Search": [{
                        "Title": "Love Actually",
                        "Year": "2003",
                        "imdbID": "tt0314331",
                        "Type": "movie",
                        "Poster": "N/A"
                      }],
                      "totalResults": "11",
                      "Response": "True"
                    }
                    """.trimIndent(),
                ),
        )

        val response = api.search(apiKey = "key", query = "love", page = 2)

        assertThat(response.results?.single()?.imdbId).isEqualTo("tt0314331")
        val request = server.takeRequest()
        assertThat(request.requestUrl?.queryParameter("s")).isEqualTo("love")
        assertThat(request.requestUrl?.queryParameter("page")).isEqualTo("2")
    }
}

package com.myarulin.movies_test.data.remote

class OmdbException(message: String) : IllegalStateException(message)

class MissingApiKeyException : IllegalStateException(
    "OMDb API key is missing. Add OMDB_API_KEY to local.properties.",
)

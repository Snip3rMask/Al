package msr.atsulab.app.data.network.apollo

import com.apollographql.apollo3.ApolloClient

interface ApolloHandler {
    val apolloClient: ApolloClient
}
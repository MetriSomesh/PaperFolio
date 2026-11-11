package com.paperfolio

import com.paperfolio.data.remote.YahooFinanceApi
import org.junit.Test

class AppStartupTest {

    @Test
    fun testYahooApiCreate() {
        println("Testing YahooFinanceApi.create()...")
        val api = YahooFinanceApi.create()
        println("YahooFinanceApi created successfully: $api")
    }
}

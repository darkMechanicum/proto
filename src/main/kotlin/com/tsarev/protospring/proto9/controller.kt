package com.tsarev.protospring.proto9

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

@RestController
open class ProtoController(
    @Qualifier("testApiFeign") private val feign: TestFeign
) : TestFeign {

    override fun test1(txid: String): TestData {
        feign.test2(txid);
        return feign.test2(txid);
    }

    override fun test2(txid: String): TestData {
        Thread.sleep(1000L)
        return TestData().apply { this.some = "some" }
    }
}

@Service
open class TestLoop(
    @Qualifier("testApiFeign") private val feign: TestFeign
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        object : Thread() {
            override fun run() {
                while (true) {
                    sleep(1000L)
                    tx("1")
                    tx("2")
                }
            }

            private fun tx(tx: String) {
                val start = System.currentTimeMillis()
                println("[$start] Calling test1 with tx $tx")
                feign.test1(tx);
                val end = System.currentTimeMillis()
                println("[$end] Calling test1 with tx $tx, taken ${end - start}ms")
            }
        }.start()
    }

}
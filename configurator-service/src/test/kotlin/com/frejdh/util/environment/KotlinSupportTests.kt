package com.frejdh.util.environment

import com.frejdh.util.environment.test.helper.ParserTests
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KotlinSupportTests {

    @Test
    fun happy() {
        Assertions.assertEquals(50, Config.getInteger("env.test1.test-of-env-int1", 10)) // Should be found
        Assertions.assertEquals(50, Config.getInteger("property.does.not.exist", 50)) // Correct default value
        Assertions.assertNull(Config.getInteger("property.does.not.exist"))
        Assertions.assertThrows(IllegalArgumentException::class.java) { Config.get("env.test1.test-of-env-int1", ParserTests::class.java) } // Should throw
    }
}


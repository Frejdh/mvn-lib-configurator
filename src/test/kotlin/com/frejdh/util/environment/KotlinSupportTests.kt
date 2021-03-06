package com.frejdh.util.environment

import org.junit.Assert
import org.junit.Test

class KotlinSupportTests {

    @Test
    fun happy() {
        Assert.assertEquals(50, Config.getInteger("env.test1.test-of-env-int1", 10)) // Should be found
        Assert.assertEquals(50, Config.getInteger("property.does.not.exist", 50)) // Correct default value
        Assert.assertNull(Config.getInteger("property.does.not.exist"))
        Assert.assertThrows<IllegalArgumentException>(IllegalArgumentException::class.java) { Config.get("env.test1.test-of-env-int1", PropertyParserTests::class.java) } // Should throw
    }
}


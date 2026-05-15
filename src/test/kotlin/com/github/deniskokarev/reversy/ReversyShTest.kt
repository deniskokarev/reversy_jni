package com.github.deniskokarev.reversy

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

class ReversyShTest {
    @Test
    fun testReversyScript() {
        val cDir = File("/app/reversy_git/c")
        val build = ProcessBuilder("make", "-C", cDir.absolutePath)
            .redirectErrorStream(true).start()
        assertEquals(0, build.waitFor(), "make should succeed")
        val run = ProcessBuilder("./reversy.sh")
            .directory(cDir).redirectErrorStream(true).start()
        assertEquals(0, run.waitFor(), "reversy.sh should exit 0")
    }
}

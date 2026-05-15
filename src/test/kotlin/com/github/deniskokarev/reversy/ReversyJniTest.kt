package com.github.deniskokarev.reversy

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ReversyJniTest {
    private fun m(c: Class<*>, name: String, n: Int) =
        c.methods.first { it.name == name && it.parameterCount == n }

    @Test
    fun testNativeLibraryLoads() {
        // Verify the native library is loadable — a pure Kotlin stub would fail here
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyGame")
        val game = c.getDeclaredConstructor().newInstance()
        // If no native library, the constructor triggers System.loadLibrary which throws UnsatisfiedLinkError
        // Also verify a native method actually works (not just class loading)
        val board = m(c, "getBoard", 0).invoke(game)
        assertNotNull(board, "getBoard should return non-null via JNI")
    }

    @Test
    fun testInitGameAndGetBoard() {
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyGame")
        val game = c.getDeclaredConstructor().newInstance()
        @Suppress("UNCHECKED_CAST")
        val board = m(c, "getBoard", 0).invoke(game) as Array<ByteArray>
        assertEquals(8, board.size)
        for (row in board) assertEquals(8, row.size)
    }

    @Test
    fun testSetBoardGetBoardCountChips() {
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyGame")
        val game = c.getDeclaredConstructor().newInstance()
        val board = Array(8) { ByteArray(8) }
        board[3][3] = 1; board[4][3] = -1
        board[3][4] = -1; board[4][4] = 1
        m(c, "setBoard", 1).invoke(game, board as Any)
        @Suppress("UNCHECKED_CAST")
        val result = m(c, "getBoard", 0).invoke(game) as Array<ByteArray>
        assertEquals(1.toByte(), result[3][3], "WHITE at 3,3")
        assertEquals((-1).toByte(), result[4][3], "BLACK at 4,3")
        val wCount = m(c, "countChips", 1).invoke(game, 1.toByte()) as Int
        val bCount = m(c, "countChips", 1).invoke(game, (-1).toByte()) as Int
        assertEquals(2, wCount, "2 white chips")
        assertEquals(2, bCount, "2 black chips")
    }

    @Test
    fun testMakeTurnFlipsChips() {
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyGame")
        val game = c.getDeclaredConstructor().newInstance()
        // Set up standard Othello opening: white at (3,3)/(4,4), black at (3,4)/(4,3)
        val board = Array(8) { ByteArray(8) }
        board[3][3] = 1; board[3][4] = -1
        board[4][3] = -1; board[4][4] = 1
        m(c, "setBoard", 1).invoke(game, board as Any)
        val bBefore = m(c, "countChips", 1).invoke(game, (-1).toByte()) as Int
        val wBefore = m(c, "countChips", 1).invoke(game, 1.toByte()) as Int
        // Use getPossibleTurns to get a valid move (avoids x/y convention assumptions)
        @Suppress("UNCHECKED_CAST")
        val turns = m(c, "getPossibleTurns", 1).invoke(game, (-1).toByte()) as List<*>
        assertTrue(turns.isNotEmpty(), "black should have valid moves")
        val firstTurn = turns[0]!!
        val x = firstTurn.javaClass.methods.first { it.name == "getX" }.invoke(firstTurn) as Int
        val y = firstTurn.javaClass.methods.first { it.name == "getY" }.invoke(firstTurn) as Int
        // Execute the move
        m(c, "makeTurn", 3).invoke(game, x, y, (-1).toByte())
        // After the move, black should have more chips (placed + flipped)
        val bAfter = m(c, "countChips", 1).invoke(game, (-1).toByte()) as Int
        val wAfter = m(c, "countChips", 1).invoke(game, 1.toByte()) as Int
        assertTrue(bAfter > bBefore, "black chips should increase after move (was $bBefore, now $bAfter)")
        assertTrue(wAfter < wBefore, "white chips should decrease after flip (was $wBefore, now $wAfter)")
    }

    @Test
    fun testGetPossibleTurns() {
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyGame")
        val game = c.getDeclaredConstructor().newInstance()
        val board = Array(8) { ByteArray(8) }
        board[3][3] = 1; board[3][4] = -1
        board[4][3] = -1; board[4][4] = 1
        m(c, "setBoard", 1).invoke(game, board as Any)
        @Suppress("UNCHECKED_CAST")
        val turns = m(c, "getPossibleTurns", 1).invoke(game, (-1).toByte()) as List<*>
        assertFalse(turns.isEmpty(), "black should have possible moves in standard opening")
        assertTrue(turns.size == 4, "standard opening has exactly 4 valid moves for each color")
    }

    @Test
    fun testIsGameOver() {
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyGame")
        val game = c.getDeclaredConstructor().newInstance()
        // Empty board — no moves for either color — game should be over
        val emptyBoard = Array(8) { ByteArray(8) }
        m(c, "setBoard", 1).invoke(game, emptyBoard as Any)
        val overOnEmpty = m(c, "isGameOver", 0).invoke(game) as Boolean
        assertTrue(overOnEmpty, "game should be over on empty board (no valid moves)")
        // Standard opening — game should NOT be over
        val board = Array(8) { ByteArray(8) }
        board[3][3] = 1; board[3][4] = -1
        board[4][3] = -1; board[4][4] = 1
        m(c, "setBoard", 1).invoke(game, board as Any)
        val overOnOpening = m(c, "isGameOver", 0).invoke(game) as Boolean
        assertFalse(overOnOpening, "game should not be over at standard opening")
    }

    @Test
    fun testAiFindsBestTurn() {
        val c = Class.forName("com.github.deniskokarev.reversy.ReversyAI")
        val ai = c.getDeclaredConstructor().newInstance()
        val board = Array(8) { ByteArray(8) }
        board[3][3] = 1; board[3][4] = -1
        board[4][3] = -1; board[4][4] = 1
        val turn = c.methods.first { it.name == "findBestTurn" && it.parameterCount == 3 }
            .invoke(ai, board as Any, (-1).toByte(), 3)
        assertNotNull(turn, "AI should find a valid turn")
        // Verify the turn has valid coordinates (0-7 range)
        val turnClass = turn!!.javaClass
        val x = turnClass.methods.first { it.name == "getX" }.invoke(turn) as Int
        val y = turnClass.methods.first { it.name == "getY" }.invoke(turn) as Int
        assertTrue(x in 0..7, "turn x should be in 0..7, got $x")
        assertTrue(y in 0..7, "turn y should be in 0..7, got $y")
    }
}

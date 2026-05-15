package com.github.deniskokarev.reversy

class ReversyAI {
    fun findBestTurn(board: Array<ByteArray>, color: Byte, depth: Int): Turn? {
        val flat = ByteArray(64)
        for (i in 0 until 8)
            for (j in 0 until 8)
                flat[i * 8 + j] = board[i][j]
        return NativeLib.nativeFindBestTurn(flat, color, depth)
    }
}

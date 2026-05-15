package com.github.deniskokarev.reversy

class ReversyGame {
    companion object {
        const val BOARD_SIZE = 8
    }

    private var handle: Long = NativeLib.nativeInitGame()

    fun makeTurn(x: Int, y: Int, color: Byte): Int =
        NativeLib.nativeMakeTurn(handle, x, y, color)

    fun validateTurn(x: Int, y: Int, color: Byte): Int =
        NativeLib.nativeValidateTurn(handle, x, y, color)

    fun getPossibleTurns(color: Byte): List<Turn> =
        NativeLib.nativeGetPossibleTurns(handle, color).toList()

    fun countChips(color: Byte): Int =
        NativeLib.nativeCountChips(handle, color)

    fun isGameOver(): Boolean =
        NativeLib.nativeIsGameOver(handle)

    fun getBoard(): Array<ByteArray> {
        val flat = NativeLib.nativeGetBoard(handle)
        return Array(BOARD_SIZE) { i ->
            ByteArray(BOARD_SIZE) { j -> flat[i * BOARD_SIZE + j] }
        }
    }

    fun setBoard(board: Array<ByteArray>) {
        val flat = ByteArray(BOARD_SIZE * BOARD_SIZE)
        for (i in 0 until BOARD_SIZE)
            for (j in 0 until BOARD_SIZE)
                flat[i * BOARD_SIZE + j] = board[i][j]
        NativeLib.nativeSetBoard(handle, flat)
    }

    protected fun finalize() {
        if (handle != 0L) {
            NativeLib.nativeFreeGame(handle)
            handle = 0
        }
    }
}

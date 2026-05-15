package com.github.deniskokarev.reversy

internal object NativeLib {
    init {
        System.loadLibrary("reversyjni")
    }

    external fun nativeInitGame(): Long
    external fun nativeFreeGame(handle: Long)
    external fun nativeMakeTurn(handle: Long, x: Int, y: Int, color: Byte): Int
    external fun nativeValidateTurn(handle: Long, x: Int, y: Int, color: Byte): Int
    external fun nativeGetPossibleTurns(handle: Long, color: Byte): Array<Turn>
    external fun nativeCountChips(handle: Long, color: Byte): Int
    external fun nativeIsGameOver(handle: Long): Boolean
    external fun nativeGetBoard(handle: Long): ByteArray
    external fun nativeSetBoard(handle: Long, board: ByteArray)
    external fun nativeFindBestTurn(board: ByteArray, color: Byte, depth: Int): Turn?
}

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "game.h"
#include "minimax.h"

JNIEXPORT jlong JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeInitGame(
    JNIEnv *env, jobject obj) {
    GAME_STATE *s = (GAME_STATE *)calloc(1, sizeof(GAME_STATE));
    return (jlong)(intptr_t)s;
}

JNIEXPORT void JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeFreeGame(
    JNIEnv *env, jobject obj, jlong handle) {
    if (handle) free((void *)(intptr_t)handle);
}

JNIEXPORT jint JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeMakeTurn(
    JNIEnv *env, jobject obj, jlong handle, jint x, jint y, jbyte color) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    GAME_TURN t = { .color = (CHIP_COLOR)color, .x = (signed char)x, .y = (signed char)y };
    return make_turn(s, &t);
}

JNIEXPORT jint JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeValidateTurn(
    JNIEnv *env, jobject obj, jlong handle, jint x, jint y, jbyte color) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    GAME_TURN t = { .color = (CHIP_COLOR)color, .x = (signed char)x, .y = (signed char)y };
    return validate_turn(s, &t);
}

JNIEXPORT jobjectArray JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeGetPossibleTurns(
    JNIEnv *env, jobject obj, jlong handle, jbyte color) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    GAME_TURN turns[MAX_DIM * MAX_DIM];
    int n = make_turn_list(turns, s, (CHIP_COLOR)color);
    jclass tc = (*env)->FindClass(env, "com/github/deniskokarev/reversy/Turn");
    jmethodID ctor = (*env)->GetMethodID(env, tc, "<init>", "(IIB)V");
    jobjectArray arr = (*env)->NewObjectArray(env, n, tc, NULL);
    for (int i = 0; i < n; i++) {
        jobject o = (*env)->NewObject(env, tc, ctor,
            (jint)turns[i].x, (jint)turns[i].y, (jbyte)turns[i].color);
        (*env)->SetObjectArrayElement(env, arr, i, o);
        (*env)->DeleteLocalRef(env, o);
    }
    return arr;
}

JNIEXPORT jint JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeCountChips(
    JNIEnv *env, jobject obj, jlong handle, jbyte color) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    return chips_count(s, (CHIP_COLOR)color);
}

JNIEXPORT jboolean JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeIsGameOver(
    JNIEnv *env, jobject obj, jlong handle) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    return game_is_over(s) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeGetBoard(
    JNIEnv *env, jobject obj, jlong handle) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    jbyteArray res = (*env)->NewByteArray(env, MAX_DIM * MAX_DIM);
    jbyte buf[MAX_DIM * MAX_DIM];
    for (int i = 0; i < MAX_DIM; i++)
        for (int j = 0; j < MAX_DIM; j++)
            buf[i * MAX_DIM + j] = (jbyte)s->b[i][j];
    (*env)->SetByteArrayRegion(env, res, 0, MAX_DIM * MAX_DIM, buf);
    return res;
}

JNIEXPORT void JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeSetBoard(
    JNIEnv *env, jobject obj, jlong handle, jbyteArray board) {
    GAME_STATE *s = (GAME_STATE *)(intptr_t)handle;
    jbyte *data = (*env)->GetByteArrayElements(env, board, NULL);
    for (int i = 0; i < MAX_DIM; i++)
        for (int j = 0; j < MAX_DIM; j++)
            s->b[i][j] = (CHIP_COLOR)data[i * MAX_DIM + j];
    (*env)->ReleaseByteArrayElements(env, board, data, JNI_ABORT);
}

JNIEXPORT jobject JNICALL Java_com_github_deniskokarev_reversy_NativeLib_nativeFindBestTurn(
    JNIEnv *env, jobject obj, jbyteArray board, jbyte color, jint depth) {
    GAME_STATE s;
    memset(&s, 0, sizeof(s));
    jbyte *data = (*env)->GetByteArrayElements(env, board, NULL);
    for (int i = 0; i < MAX_DIM; i++)
        for (int j = 0; j < MAX_DIM; j++)
            s.b[i][j] = (CHIP_COLOR)data[i * MAX_DIM + j];
    (*env)->ReleaseByteArrayElements(env, board, data, JNI_ABORT);

    GAME_TURN best;
    GAME_SCORE score = find_best_turn(&best, &s, (CHIP_COLOR)color, (int)depth);
    if (score == GAME_SCORE_MIN) return NULL;

    jclass tc = (*env)->FindClass(env, "com/github/deniskokarev/reversy/Turn");
    jmethodID ctor = (*env)->GetMethodID(env, tc, "<init>", "(IIB)V");
    return (*env)->NewObject(env, tc, ctor,
        (jint)best.x, (jint)best.y, (jbyte)best.color);
}

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "DeepSeekBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeInitEngine(
    JNIEnv *env,
    jobject,
    jstring configPath) {

    const char *config_path = env->GetStringUTFChars(configPath, nullptr);
    LOGI("Initializing DeepSeek engine with config: %s", config_path);
    env->ReleaseStringUTFChars(configPath, config_path);
    return 12345;
}

JNIEXPORT void JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeDestroy(
    JNIEnv *env,
    jobject) {
    LOGI("Destroying DeepSeek engine");
}

JNIEXPORT jstring JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeCreateSession(
    JNIEnv *env,
    jobject,
    jstring name) {
    const char *session_name = env->GetStringUTFChars(name, nullptr);
    LOGI("Creating session: %s", session_name);
    env->ReleaseStringUTFChars(name, session_name);
    char result[64];
    snprintf(result, sizeof(result), "session_%ld", time(nullptr));
    return env->NewStringUTF(result);
}

JNIEXPORT jstring JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeListSessions(
    JNIEnv *env,
    jobject) {
    LOGI("Listing sessions");
    return env->NewStringUTF("[]");
}

JNIEXPORT jstring JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeSendMessage(
    JNIEnv *env,
    jobject,
    jstring sessionId,
    jstring message) {

    const char *sid = env->GetStringUTFChars(sessionId, nullptr);
    const char *msg = env->GetStringUTFChars(message, nullptr);

    LOGI("Sending message to session %s: %s", sid, msg);

    env->ReleaseStringUTFChars(sessionId, sid);
    env->ReleaseStringUTFChars(message, msg);

    const char *response = "🤖 DeepSeek Agent 已收到你的消息!\n\n"
                           "支持的功能:\n"
                           "• /help - 显示帮助\n"
                           "• /mode - 切换模式\n"
                           "• /clear - 清屏\n"
                           "• /checkpoint - 创建检查点\n\n"
                           "请输入你的问题或任务。";
    return env->NewStringUTF(response);
}

JNIEXPORT jstring JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeGetSessionHistory(
    JNIEnv *env,
    jobject,
    jstring sessionId) {
    const char *sid = env->GetStringUTFChars(sessionId, nullptr);
    LOGI("Getting session history: %s", sid);
    env->ReleaseStringUTFChars(sessionId, sid);
    return env->NewStringUTF("[]");
}

JNIEXPORT jboolean JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeDeleteSession(
    JNIEnv *env,
    jobject,
    jstring sessionId) {
    const char *sid = env->GetStringUTFChars(sessionId, nullptr);
    LOGI("Deleting session: %s", sid);
    env->ReleaseStringUTFChars(sessionId, sid);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeCreateCheckpoint(
    JNIEnv *env,
    jobject,
    jstring sessionId,
    jstring description) {
    const char *sid = env->GetStringUTFChars(sessionId, nullptr);
    const char *desc = env->GetStringUTFChars(description, nullptr);
    LOGI("Creating checkpoint for session %s: %s", sid, desc);
    env->ReleaseStringUTFChars(sessionId, sid);
    env->ReleaseStringUTFChars(description, desc);
    char result[64];
    snprintf(result, sizeof(result), "checkpoint_%ld", time(nullptr));
    return env->NewStringUTF(result);
}

JNIEXPORT jstring JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeListCheckpoints(
    JNIEnv *env,
    jobject,
    jstring sessionId) {
    const char *sid = env->GetStringUTFChars(sessionId, nullptr);
    LOGI("Listing checkpoints for session: %s", sid);
    env->ReleaseStringUTFChars(sessionId, sid);
    return env->NewStringUTF("[]");
}

JNIEXPORT jboolean JNICALL
Java_com_termux_deepseek_DeepSeekBridge_nativeRestoreCheckpoint(
    JNIEnv *env,
    jobject,
    jstring checkpointId) {
    const char *cid = env->GetStringUTFChars(checkpointId, nullptr);
    LOGI("Restoring checkpoint: %s", cid);
    env->ReleaseStringUTFChars(checkpointId, cid);
    return JNI_TRUE;
}

}

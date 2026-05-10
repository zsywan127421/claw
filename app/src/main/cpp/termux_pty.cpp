#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>

#define LOG_TAG "TermuxPTY"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL
Java_com_termux_terminal_JNI_createSubprocess(JNIEnv *env, jobject obj, jstring cmd, jstring cwd,
    jobjectArray envVars, jobjectArray args, jintArray processId, jint rows, jint cols,
    jint pixelWidth, jint pixelHeight) {

    const char *cmd_str = env->GetStringUTFChars(cmd, NULL);
    int masterFd = open("/dev/ptmx", O_RDWR | O_NOCTTY);
    if (masterFd < 0) { LOGE("Failed to open ptmx: %s", strerror(errno)); return -1; }
    grantpt(masterFd); unlockpt(masterFd);
    char *ptsName = ptsname(masterFd);
    if (ptsName == NULL) { close(masterFd); return -1; }

    pid_t pid = fork();
    if (pid < 0) { close(masterFd); return -1; }

    if (pid == 0) {
        close(masterFd);
        int slaveFd = open(ptsName, O_RDWR | O_NOCTTY);
        if (slaveFd < 0) _exit(1);
        setsid();
        ioctl(slaveFd, TIOCSCTTY, 0);
        struct winsize ws = { (unsigned short)rows, (unsigned short)cols, (unsigned short)pixelWidth, (unsigned short)pixelHeight };
        ioctl(slaveFd, TIOCSWINSZ, &ws);
        dup2(slaveFd, STDIN_FILENO);
        dup2(slaveFd, STDOUT_FILENO);
        dup2(slaveFd, STDERR_FILENO);
        if (slaveFd > 2) close(slaveFd);
        if (cwd != NULL) {
            const char *cwd_str = env->GetStringUTFChars(cwd, NULL);
            chdir(cwd_str);
            env->ReleaseStringUTFChars(cwd, cwd_str);
        }
        if (args != NULL) {
            jsize argCount = env->GetArrayLength(args);
            char **argv = (char**)malloc((argCount + 1) * sizeof(char*));
            for (int i = 0; i < argCount; i++) {
                jstring arg = (jstring)env->GetObjectArrayElement(args, i);
                argv[i] = strdup(env->GetStringUTFChars(arg, NULL));
                env->ReleaseStringUTFChars(arg, argv[i]);
            }
            argv[argCount] = NULL;
            execvp(argv[0], argv);
        }
        _exit(1);
    }

    env->ReleaseStringUTFChars(cmd, cmd_str);
    if (processId != NULL) {
        jint *pidArr = env->GetIntArrayElements(processId, NULL);
        pidArr[0] = pid;
        env->ReleaseIntArrayElements(processId, pidArr, 0);
    }
    return masterFd;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyWindowSize(JNIEnv *env, jobject obj, jint fd, jint rows, jint cols, jint pixelWidth, jint pixelHeight) {
    struct winsize ws = { (unsigned short)rows, (unsigned short)cols, (unsigned short)pixelWidth, (unsigned short)pixelHeight };
    ioctl(fd, TIOCSWINSZ, &ws);
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_waitFor(JNIEnv *env, jobject obj, jint processId) {
    int status; waitpid(processId, &status, 0); return WEXITSTATUS(status);
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_close(JNIEnv *env, jobject obj, jint fd) { close(fd); }

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_read(JNIEnv *env, jobject obj, jint fd, jbyteArray buffer, jint length) {
    jbyte *buf = env->GetByteArrayElements(buffer, NULL);
    int bytesRead = read(fd, buf, length);
    env->ReleaseByteArrayElements(buffer, buf, 0);
    return bytesRead;
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_write(JNIEnv *env, jobject obj, jint fd, jbyteArray buffer, jint length) {
    jbyte *buf = env->GetByteArrayElements(buffer, NULL);
    int bytesWritten = write(fd, buf, length);
    env->ReleaseByteArrayElements(buffer, buf, 0);
    return bytesWritten;
}

}

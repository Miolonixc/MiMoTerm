#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <pty.h>

#define LOG_TAG "PtyNative"
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct {
    int master_fd;
    pid_t child_pid;
} PtyProcess;

static PtyProcess processes[64];
static int process_count = 0;

static int find_process_index(int master_fd) {
    for (int i = 0; i < process_count; i++) {
        if (processes[i].master_fd == master_fd) {
            return i;
        }
    }
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_mimoterm_core_terminal_Pty_nativeCreateSubprocess(
    JNIEnv *env,
    jobject thiz,
    jstring command,
    jstring cwd,
    jobjectArray args,
    jobjectArray envVars,
    jintArray processIdArray,
    jint rows,
    jint cols
) {
    const char *cmd = (*env)->GetStringUTFChars(env, command, NULL);
    const char *workDir = cwd ? (*env)->GetStringUTFChars(env, cwd, NULL) : NULL;

    // Open PTY master
    int master = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (master < 0) {
        LOGE("Failed to open /dev/ptmx: %s", strerror(errno));
        (*env)->ReleaseStringUTFChars(env, command, cmd);
        if (workDir) (*env)->ReleaseStringUTFChars(env, cwd, workDir);
        return -1;
    }

    // Grant and unlock slave
    grantpt(master);
    unlockpt(master);

    char slave_name[256];
    ptsname_r(master, slave_name, sizeof(slave_name));
    LOGI("Slave device: %s", slave_name);

    // Configure terminal
    struct termios tios;
    tcgetattr(master, &tios);
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= ~(IXON | IXOFF | IXANY);
    tios.c_lflag |= (ICANON | ECHO | ISIG);
    tios.c_oflag |= OPOST;
    tcsetattr(master, TCSANOW, &tios);

    // Set window size
    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = rows;
    ws.ws_col = cols;
    ws.ws_xpixel = cols * 8;
    ws.ws_ypixel = rows * 16;
    ioctl(master, TIOCSWINSZ, &ws);

    // Fork
    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork() failed: %s", strerror(errno));
        close(master);
        (*env)->ReleaseStringUTFChars(env, command, cmd);
        if (workDir) (*env)->ReleaseStringUTFChars(env, cwd, workDir);
        return -1;
    }

    if (pid == 0) {
        // Child process
        close(master);

        setsid();

        int slave = open(slave_name, O_RDWR);
        if (slave < 0) {
            LOGE("Failed to open slave: %s", strerror(errno));
            _exit(1);
        }

        dup2(slave, 0);
        dup2(slave, 1);
        dup2(slave, 2);
        if (slave > 2) close(slave);

        // Set window size on slave
        ioctl(0, TIOCSWINSZ, &ws);

        // Set environment variables
        if (envVars) {
            int envCount = (*env)->GetArrayLength(env, envVars);
            for (int i = 0; i < envCount; i++) {
                jstring envVar = (jstring)(*env)->GetObjectArrayElement(env, envVars, i);
                const char *envStr = (*env)->GetStringUTFChars(env, envVar, NULL);
                putenv(strdup(envStr));
                (*env)->ReleaseStringUTFChars(env, envVar, envStr);
            }
        }

        // Change working directory
        if (workDir) {
            if (chdir(workDir) != 0) {
                LOGE("chdir(%s) failed: %s", workDir, strerror(errno));
            }
        }

        // Build command arguments
        const char **argv;
        int argc;

        if (args) {
            int argCount = (*env)->GetArrayLength(env, args);
            argc = argCount + 1;
            argv = (const char **)malloc(sizeof(char *) * (argc + 1));
            argv[0] = cmd;
            for (int i = 0; i < argCount; i++) {
                jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
                argv[i + 1] = (*env)->GetStringUTFChars(env, arg, NULL);
            }
        } else {
            argc = 1;
            argv = (const char **)malloc(sizeof(char *) * 2);
            argv[0] = cmd;
        }
        argv[argc] = NULL;

        // Execute shell
        execvp(argv[0], (char *const *)argv);

        // If exec fails, try with /bin/sh -c
        const char *fallback_args[] = { "/system/bin/sh", "-c", cmd, NULL };
        execvp("/system/bin/sh", (char *const *)fallback_args);

        LOGE("execvp failed: %s", strerror(errno));
        _exit(1);
    }

    // Parent process
    (*env)->ReleaseStringUTFChars(env, command, cmd);
    if (workDir) (*env)->ReleaseStringUTFChars(env, cwd, workDir);

    // Store process info
    if (process_count < 64) {
        processes[process_count].master_fd = master;
        processes[process_count].child_pid = pid;
        process_count++;
    }

    // Set process ID in Java array
    (*env)->SetIntArrayRegion(env, processIdArray, 0, 1, &pid);

    LOGI("Created PTY process: pid=%d, master_fd=%d", pid, master);

    return master;
}

JNIEXPORT void JNICALL
Java_com_mimoterm_core_terminal_Pty_nativeSetWindowSize(
    JNIEnv *env,
    jobject thiz,
    jint fd,
    jint rows,
    jint cols
) {
    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = rows;
    ws.ws_col = cols;
    ws.ws_xpixel = cols * 8;
    ws.ws_ypixel = rows * 16;
    ioctl(fd, TIOCSWINSZ, &ws);
}

JNIEXPORT jint JNICALL
Java_com_mimoterm_core_terminal_Pty_nativeWaitFor(
    JNIEnv *env,
    jobject thiz,
    jint fd
) {
    int idx = find_process_index(fd);
    if (idx < 0) return -1;

    int status;
    waitpid(processes[idx].child_pid, &status, 0);

    // Clean up
    close(fd);
    processes[idx] = processes[--process_count];

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    return -1;
}

JNIEXPORT void JNICALL
Java_com_mimoterm_core_terminal_Pty_nativeClose(
    JNIEnv *env,
    jobject thiz,
    jint fd
) {
    int idx = find_process_index(fd);
    if (idx >= 0) {
        kill(processes[idx].child_pid, SIGHUP);
        close(fd);
        processes[idx] = processes[--process_count];
    }
}

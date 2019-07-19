#pragma once

#include <jni.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif
#include "quickjs-libc.h"
#include "quickjs.h"
#ifdef __cplusplus
};
#endif
#define MYTAG "QuickJs"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,MYTAG ,__VA_ARGS__)
#if defined(__cplusplus)


#include <vector>
#include <string>
#include <algorithm>
#include <tuple>
#include <functional>
#include <unordered_map>
#include <cassert>
#include <memory>
#include <string_view>


extern "C"
#endif

JSValue js_print(JSContext *ctx, JSValueConst this_val,
                 int argc, JSValueConst *argv);
#if defined(__cplusplus)
extern "C"
#endif
JNIEXPORT jboolean JNICALL
Java_com_quickjs_QuickJs_open(JNIEnv *env, jclass type);
#ifdef __MYHPP
void ToastText(const char* str);

#endif
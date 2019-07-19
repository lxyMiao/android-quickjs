
#include "Toast.h"

JavaVM *mVM = NULL;
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_quickjs_QuickJs_open(JNIEnv *env, jclass type) {
    env->GetJavaVM(&mVM);
    return true;

}

jobject getGlobalContext(JNIEnv *env) {
    //获取Activity Thread的实例对象
    jclass activityThread = env->FindClass("android/app/ActivityThread");
    jmethodID currentActivityThread = env->GetStaticMethodID(activityThread,
                                                             "currentActivityThread",
                                                             "()Landroid/app/ActivityThread;");
    jobject at = env->CallStaticObjectMethod(activityThread, currentActivityThread);
    //获取Application，也就是全局的Context
    jmethodID getApplication = env->GetMethodID(activityThread, "getApplication",
                                                "()Landroid/app/Application;");
    jobject context = env->CallObjectMethod(at, getApplication);
    return context;
}

static void MyToast(JNIEnv *env, const char *str) {
    jobject context = getGlobalContext(env);
    jclass Toast = env->FindClass("android/widget/Toast");
    jstring st = env->NewStringUTF(str);
    jmethodID method = env->GetStaticMethodID(Toast, "makeText",
                                              "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;");
    jobject obj = env->CallStaticObjectMethod(Toast, method, context, st, 1);
    jmethodID show = env->GetMethodID(Toast, "show", "()V");
    env->CallVoidMethod(obj, show);
    env->DeleteLocalRef(st);
    env->DeleteLocalRef(Toast);
    env->DeleteLocalRef(obj);
}

void ToastText(const char *str) {
    JNIEnv *env;
    if (mVM->AttachCurrentThread(&env, NULL) != JNI_OK) {
        LOGD("get env error");
        return;
    }
    MyToast(env, str);
}


extern "C"
JSValue js_print(JSContext *ctx, JSValueConst this_val,
                 int argc, JSValueConst *argv) {
    JNIEnv *env;
    if (mVM->AttachCurrentThread(&env, NULL) != JNI_OK) {
        LOGD("get env error");
        return JS_UNINITIALIZED;
    }
    int i;
    const char *str;
    std::string strpp;
    for (i = 0; i < argc; i++) {
        if (i != 0)
            strpp.append(" ");
        str = JS_ToCString(ctx, argv[i]);
        if (!str)
            return JS_EXCEPTION;
        strpp.append(str);
        JS_FreeCString(ctx, str);
    }
    strpp.append("\n");
    LOGD(strpp.c_str());
    MyToast(env, strpp.c_str());
    return JS_UNDEFINED;
}


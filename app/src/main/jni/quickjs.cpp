#include <jni.h>
#include <string>
#include <string_view>
#include <cstdio>
#include <android/log.h>
#include <cerrno>
#include "quickjspp.hpp"
#include "quickjs/cutils.h"

/*
jobject getGlobalContext(JNIEnv *env)
{
    //获取Activity Thread的实例对象
    jclass activityThread = env->FindClass("android/app/ActivityThread");
    jmethodID currentActivityThread = env->GetStaticMethodID(activityThread, "currentActivityThread", "()Landroid/app/ActivityThread;");
    jobject at = env->CallStaticObjectMethod(activityThread, currentActivityThread);
    //获取Application，也就是全局的Context
    jmethodID getApplication = env->GetMethodID(activityThread, "getApplication", "()Landroid/app/Application;");
    jobject context = env->CallObjectMethod(at, getApplication);
    return context;
}
*/
//JSRuntime * rt;
//JSContext * ctx;
using namespace qjs;
//Context * context ;
/*static void MyToast(JNIEnv * env,const char* str){
    jobject context=getGlobalContext(env);
    jclass Toast=env->FindClass("android/widget/Toast");
    jstring st=env->NewStringUTF(str);
    jmethodID method=env->GetStaticMethodID(Toast,"makeText","(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;");
    jobject obj=env->CallStaticObjectMethod(Toast,method,context,st,1);
    jmethodID show=env->GetMethodID(Toast,"show","()V");
    env->CallVoidMethod(obj,show);
    env->DeleteLocalRef(st);
    env->DeleteLocalRef(Toast);
    env->DeleteLocalRef(obj);
}
static JSValue JS_Print(JSContext *ctx, JSValueConst this_val,
                        int argc, JSValueConst *argv){
    JNIEnv *env;
    if(mVm->AttachCurrentThread(&env,NULL)!=JNI_OK){
        LOGD("get env error");
        return JS_UNINITIALIZED;
    }
    int i;
    const char *str;

    for(i = 0; i < argc; i++) {
        if (i != 0)
            putchar(' ');
        str = JS_ToCString(ctx, argv[i]);
        if (!str)
            return JS_EXCEPTION;
        MyToast(env,str);
        JS_FreeCString(ctx, str);
    }
    putchar('\n');
    return JS_UNDEFINED;
}*/

static jmethodID throwable_tostring_method = NULL;
static jclass throwable_class, Api_class = NULL;
static jmethodID get_message_method, call_method, tostring_method, new_Instance = NULL;
static JSClassID mJobject_class_id = NULL;
static JSClassDef js_jobject_class = {
        "JavaObject"
};

void checkError(JNIEnv *javaEnv, JSContext *cont) {
    jthrowable exp = (javaEnv)->ExceptionOccurred();

    /* Handles exception */
    if (exp != NULL) {
        jstring jstr;
        const char *cStr;

        (javaEnv)->ExceptionClear();
        jstr = static_cast<jstring>((javaEnv)->CallObjectMethod(exp, get_message_method));

        if (jstr == NULL) {
            if (throwable_tostring_method == NULL)
                throwable_tostring_method = (javaEnv)->GetMethodID(throwable_class,
                                                                   "toString",
                                                                   "()Ljava/lang/String;");
            jstr = static_cast<jstring>((javaEnv)->CallObjectMethod(exp,
                                                                    throwable_tostring_method));
        }

        cStr = (javaEnv)->GetStringUTFChars((jstr), NULL);
        //lua_settop(L, 0);
        JS_ThrowInternalError(cont, cStr);
        (javaEnv)->ReleaseStringUTFChars((jstr), cStr);
        (javaEnv)->DeleteLocalRef(exp);
        (javaEnv)->DeleteLocalRef(jstr);

    }
}

jobject *checkJavaObject(JSContext *cont, JSValueConst val) {
    //if (!isJavaObject(L, idx))
    //luaL_typerror(L, idx, "java Object");
    //return (jobject *)lua_touserdata(L, idx);
    jobject *obj;
    obj = static_cast<jobject *> (JS_GetOpaque(val, mJobject_class_id));
    if (*obj == NULL) {
        JS_ThrowInternalError(cont, "not JavaObject or is nil");
    }
    return obj;
}

JNIEnv *checkEnv(JSContext *con) {
    JNIEnv *javaEnv = (JNIEnv *) 0;

    if (JS_GetContextOpaque(con) != NULL)
        javaEnv = static_cast<JNIEnv *>(JS_GetContextOpaque(con));
    if (!javaEnv)
        JS_ThrowInternalError(con, "Invalid JNI Environment.");
    return javaEnv;
}

static JSValue pushJObject(JSContext *cont, jobject jobj) {
    jobject *userData, globalRef;

    /* Gets the JNI Environment */
    JNIEnv *javaEnv = checkEnv(cont);

    globalRef = javaEnv->NewGlobalRef(jobj);
    //checkError(javaEnv, L);
//LOGD("Java object %d %d",&javaObject,&globalRef);

    JSValue obj;
    obj = JS_NewObjectClass(cont, mJobject_class_id);
    if (JS_IsException(obj))
        return obj;
    userData = static_cast<jobject *>(js_mallocz(cont, sizeof(*userData)));
    if (!userData) {
        JS_FreeValue(cont, obj);
        return JS_EXCEPTION;
    }
    *userData = globalRef;
    JS_SetOpaque(obj, userData);
    return obj;
}

extern "C"
JSValue java_totring(JSContext *ctx, JSValueConst this_val,
                     int argc, JSValueConst *argv) {
    const char *cstr;
    jobject *jobj = static_cast<jobject *>(JS_GetOpaque(this_val, mJobject_class_id));
    if (!jobj)
        return JS_EXCEPTION;
    JNIEnv *env = checkEnv(ctx);
    jstring str = reinterpret_cast<jstring >(env->CallStaticObjectMethod(Api_class, tostring_method,
                                                                         *jobj));
    checkError(env, ctx);
    cstr = env->GetStringUTFChars(str, 0);
    std::string_view jstr = cstr;
    env->ReleaseStringUTFChars(str, cstr);
    return JS_NewString(ctx, jstr.data());
}
extern "C"
JSValue java_new(JSContext *ctx, JSValueConst this_val,
                 int argc, JSValueConst *argv) {
    jobject *jobj = static_cast<jobject *>(JS_GetOpaque(this_val, mJobject_class_id));
    if (!jobj)
        return JS_EXCEPTION;
    JNIEnv *env = checkEnv(ctx);
    jclass Clz = env->FindClass("java/lang/Class");
    if (!env->IsInstanceOf(*jobj, Clz))
        return JS_EXCEPTION;
    jclass object = env->FindClass("java/lang/Object");
    if (argc > 0) {
        jobjectArray array = env->NewObjectArray(argc, object, NULL);
        for (int i = 0; i < argc; i++) {
            JSValue temp = argv[i];
            switch (JS_VALUE_GET_TAG(temp)) {
                case JS_TAG_STRING: {
                    const char *str;
                    str = JS_ToCString(ctx, temp);
                    jstring jstr = env->NewStringUTF(str);
                    JS_FreeCString(ctx, str);
                    env->SetObjectArrayElement(array, i, jstr);
                    env->DeleteLocalRef(jstr);
                    break;
                }
                case JS_TAG_BOOL: {
                    jboolean boolean = JS_ToBool(ctx, temp);
                    env->SetObjectArrayElement(array, i, reinterpret_cast<jobject>(boolean));
                    break;
                }
                case JS_TAG_FLOAT64: {
                    jdouble jdb;
                    jclass doublec = env->FindClass("java/lang/Double");
                    jmethodID newd = env->GetMethodID(doublec, "<init>", "(D)V");
                    JS_ToFloat64(ctx, &jdb, temp);
                    jobject dobj = env->NewObject(doublec, newd, jdb);
                    LOGD("double %d", jdb);
                    checkError(env, ctx);
                    env->SetObjectArrayElement(array, i, (dobj));
                    env->DeleteLocalRef(doublec);
                    break;
                }
                case JS_TAG_INT: {
                    jint jint1;
                    jclass intc = env->FindClass("java/lang/Integer");
                    jmethodID newi = env->GetMethodID(intc, "<init>", "(I)V");
                    JS_ToInt32(ctx, &jint1, temp);

                    jobject iobj = env->NewObject(intc, newi, jint1);

                    checkError(env, ctx);
                    env->SetObjectArrayElement(array, i, (iobj));
                    env->DeleteLocalRef(intc);
                    LOGD("int %d", jint1);
                    break;
                }
                case JS_TAG_OBJECT: {
                    jobject *mobject = checkJavaObject(ctx, temp);
                    env->SetObjectArrayElement(array, i, *mobject);
                    break;
                }
            }
        }
        try {
            jobject result = env->CallStaticObjectMethod(Api_class, new_Instance, *jobj, array);
            checkError(env, ctx);
            if (result == NULL)
                return JS_NULL;
            JSValue res = pushJObject(ctx, result);
            return res;
        }
        catch (std::exception &e) {
            JS_ThrowInternalError(ctx, e.what());
            return JS_UNDEFINED;
        }
    } else {
        try {
            jobjectArray jobjArray = env->NewObjectArray(0, object, NULL);
            jobject result = env->CallStaticObjectMethod(Api_class, new_Instance, *jobj, jobjArray);
            checkError(env, ctx);
            if (result == NULL)
                return JS_NULL;
            JSValue res = pushJObject(ctx, result);
            return res;
        }
        catch (std::exception &e) {
            JS_ThrowInternalError(ctx, e.what());
            return JS_UNDEFINED;
        }
    }
}
extern "C"
JSValue java_invoke(JSContext *ctx, JSValueConst this_val,
                    int argc, JSValueConst *argv) {
    const char *method;
    jstring methodstr;
    jobject *jobj = static_cast<jobject *>(JS_GetOpaque(this_val, mJobject_class_id));
    if (!jobj)
        return JS_EXCEPTION;
    method = JS_ToCString(ctx, argv[0]);
    if (!method)
        return JS_EXCEPTION;
    JNIEnv *env = checkEnv(ctx);
    methodstr = env->NewStringUTF(method);
    checkError(env, ctx);
    jclass object = env->FindClass("java/lang/Object");
    if (argc > 1) {

        jobjectArray array = env->NewObjectArray(argc - 1, object, NULL);
        for (int i = 1; i < argc; i++) {
            JSValue temp = argv[i];
            switch (JS_VALUE_GET_TAG(temp)) {
                case JS_TAG_STRING: {
                    const char *str;
                    str = JS_ToCString(ctx, temp);
                    jstring jstr = env->NewStringUTF(str);
                    JS_FreeCString(ctx, str);
                    env->SetObjectArrayElement(array, i - 1, jstr);
                    env->DeleteLocalRef(jstr);
                    break;
                }
                case JS_TAG_BOOL: {
                    jboolean boolean = JS_ToBool(ctx, temp);
                    env->SetObjectArrayElement(array, i - 1, reinterpret_cast<jobject>(boolean));
                    break;
                }
                case JS_TAG_FLOAT64: {
                    jdouble jdb;
                    jclass doublec = env->FindClass("java/lang/Double");
                    jmethodID newd = env->GetMethodID(doublec, "<init>", "(D)V");
                    JS_ToFloat64(ctx, &jdb, temp);
                    jobject dobj = env->NewObject(doublec, newd, jdb);
                    LOGD("double %d", jdb);
                    checkError(env, ctx);
                    env->SetObjectArrayElement(array, i - 1, (dobj));
                    env->DeleteLocalRef(doublec);
                    break;
                }
                case JS_TAG_INT: {
                    jint jint1;
                    jclass intc = env->FindClass("java/lang/Integer");
                    jmethodID newi = env->GetMethodID(intc, "<init>", "(I)V");
                    JS_ToInt32(ctx, &jint1, temp);

                    jobject iobj = env->NewObject(intc, newi, jint1);

                    checkError(env, ctx);
                    env->SetObjectArrayElement(array, i - 1, (iobj));
                    env->DeleteLocalRef(intc);
                    LOGD("int %d", jint1);
                    break;
                }
                case JS_TAG_OBJECT: {
                    jobject *jobj = checkJavaObject(ctx, temp);
                    env->SetObjectArrayElement(array, i, *jobj);
                    break;
                }
            }
        }
        try {
            jobject result = env->CallStaticObjectMethod(Api_class, call_method, *jobj, methodstr,
                                                         array);
            checkError(env, ctx);
            if (result == NULL)
                return JS_NULL;
            env->DeleteLocalRef(methodstr);
            JSValue res = pushJObject(ctx, result);
            return res;
        }
        catch (std::exception &e) {
            JS_ThrowInternalError(ctx, e.what());
            return JS_UNDEFINED;
        }
    } else {
        try {
            jobjectArray jobjArray = env->NewObjectArray(0, object, NULL);
            jobject result = env->CallStaticObjectMethod(Api_class, call_method, *jobj, methodstr,
                                                         jobjArray);
            checkError(env, ctx);
            if (result == NULL)
                return JS_NULL;
            env->DeleteLocalRef(methodstr);
            JSValue res = pushJObject(ctx, result);
            return res;
        }
        catch (std::exception &e) {
            JS_ThrowInternalError(ctx, e.what());
            return JS_UNDEFINED;
        }
    }
};
extern "C"
JSValue javaBindClass(JSContext *ctx, JSValueConst this_val,
                      int argc, JSValueConst *argv) {
    const char *className;
    jstring javaClassName;
    jobject classInstance;
    JNIEnv *javaEnv;


/* Gets the JNI Environment */
    javaEnv = checkEnv(ctx);
    LOGD("get env %d", javaEnv == NULL);
/* get the string parameter */
    className = JS_ToCString(ctx, argv[0]);
    if (!className) {
        JS_FreeCString(ctx, className);
        return JS_EXCEPTION;
    }
    jmethodID forname = javaEnv->GetStaticMethodID(Api_class, "javaBindClass",
                                                   "(Ljava/lang/String;)Ljava/lang/Class;");

    javaClassName = javaEnv->NewStringUTF(className);
    checkError(javaEnv, ctx);
    classInstance = javaEnv->CallStaticObjectMethod(Api_class, forname, javaClassName);
    checkError(javaEnv, ctx);
    javaEnv->DeleteLocalRef(javaClassName);
    if (classInstance == NULL)
        return JS_NULL;
    JSValue obj = pushJObject(ctx, classInstance);
    (javaEnv)->DeleteLocalRef(classInstance);
    return obj;
}

static JSValue js_os_return(JSContext *ctx, ssize_t ret) {
    if (ret < 0)
        ret = -errno;
    return JS_NewInt64(ctx, ret);
}

static const JSCFunctionListEntry js_java_proto_funcs[] = {
        JS_CFUNC_DEF("invoke", 1, java_invoke),
        JS_CFUNC_DEF("toString", 0, java_totring),
        JS_CFUNC_DEF("new", 0, java_new)
};
static const JSCFunctionListEntry js_ja_funcs[] = {
        JS_CFUNC_DEF("bindclass", 1, javaBindClass)
};

static int js_ja_init(JSContext *ctx, JSModuleDef *m) {
    JSValue proto, obj;

    /* FILE class */
    /* the class ID is created once */
    JS_NewClassID(&mJobject_class_id);
    JS_NewClass(JS_GetRuntime(ctx), mJobject_class_id, &js_jobject_class);
    proto = JS_NewObject(ctx);
    JS_SetPropertyFunctionList(ctx, proto, js_java_proto_funcs,
                               countof(js_java_proto_funcs));
    JS_SetClassProto(ctx, mJobject_class_id, proto);


    return JS_SetModuleExportList(ctx, m, js_ja_funcs,
                                  countof(js_ja_funcs));


    /* global object */
    //JS_SetModuleExport(ctx, m, "global", JS_GetGlobalObject(ctx));
}

static void Activity_init(JSContext *context, JNIEnv *jniEnv) {
    jclass tempClass;
    if (throwable_class == NULL) {
        tempClass = (jniEnv)->FindClass("java/lang/Throwable");

        if (tempClass == NULL) {
            fprintf(stderr, "Error. Couldn't bind java class java.lang.Throwable\n");
            exit(1);
        }

        throwable_class = static_cast<jclass>((jniEnv)->NewGlobalRef(tempClass));
        (jniEnv)->DeleteLocalRef(tempClass);

        if (throwable_class == NULL) {
            fprintf(stderr, "Error. Couldn't bind java class java.lang.Throwable\n");
            exit(1);
        }
    }

    if (get_message_method == NULL) {
        get_message_method = (jniEnv)->GetMethodID(throwable_class, "getMessage",
                                                   "()Ljava/lang/String;");

        if (get_message_method == NULL) {
            fprintf(stderr,
                    "Could not find <getMessage> method in java.lang.Throwable\n");
            exit(1);
        }
    }
    if (Api_class == NULL) {
        tempClass = (jniEnv)->FindClass("com/quickjs/QuickJsApi");

        if (tempClass == NULL) {
            fprintf(stderr, "Error. Couldn't bind java class QuickJsApi\n");
            exit(1);
        }

        Api_class = static_cast<jclass>((jniEnv)->NewGlobalRef(tempClass));
        (jniEnv)->DeleteLocalRef(tempClass);

        if (Api_class == NULL) {
            fprintf(stderr, "Error. Couldn't bind java class java.lang.Throwable\n");
            exit(1);
        }
    }

    if (call_method == NULL) {
        call_method = (jniEnv)->GetStaticMethodID(Api_class, "callStaticMethod",
                                                  "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        if (call_method == NULL) {
            fprintf(stderr,
                    "Could not find <getMessage> method in java.lang.Throwable\n");
            LOGD("call is null");
            exit(1);
        }
    }

    if (tostring_method == NULL) {
        tostring_method = (jniEnv)->GetStaticMethodID(Api_class, "javaToString",
                                                      "(Ljava/lang/Object;)Ljava/lang/String;");
        if (tostring_method == NULL) {
            fprintf(stderr,
                    "Could not find tostring\n");
            LOGD("call is null");
            exit(1);
        }
    }
    if (new_Instance == NULL) {
        new_Instance = (jniEnv)->GetStaticMethodID(Api_class, "newJInstance",
                                                   "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;");
        if (new_Instance == NULL) {
            fprintf(stderr,
                    "Could not find newInstance\n");
            LOGD("call is null");
            exit(1);
        }
    }
    JS_SetContextOpaque(context, jniEnv);
    JSModuleDef *m;
    m = JS_NewCModule(context, "java", js_ja_init);
    if (!m)
        return;
    JS_AddModuleExportList(context, m, js_ja_funcs, countof(js_ja_funcs));
}

extern "C"
JSValue o_remove(JSContext *ctx, JSValueConst this_val,
                 int argc, JSValueConst *argv) {
    const char *filename;
    int ret;

    filename = JS_ToCString(ctx, argv[0]);
    if (!filename)
        return JS_EXCEPTION;
    ret = remove(filename);
    JS_FreeCString(ctx, filename);
    return js_os_return(ctx, ret);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_quickjs_QuickJs_evalString(JNIEnv *env, jclass type, jobject act, jstring str_) {
    const char *str = env->GetStringUTFChars(str_, 0);
    LOGD(str);
    JSRuntime *rt;
    Runtime runtime;
    rt = runtime.rt;

    Context cont(runtime);
    JSContext *ct = cont.ctx;
    Activity_init(ct, env);
    JS_SetModuleLoaderFunc(rt, NULL, js_module_loader, NULL);


    js_std_add_helpers(ct, 0, NULL);
    /* system modules */
    js_init_module_std(ct, "std");
    js_init_module_os(ct, "os");

    /* make 'std' and 'os' visible to non module code */
    const char *strr = "import * as std from 'std';\n"
                       // "import * as file from 'file';\n"
                       "import * as os from 'os';\n"
                       "import * as java from 'java'\n"
                       "std.global.std = std;\n"
                       "std.global.java =java ;\n"
                       "std.global.os = os;\n";

    cont.eval(strr, "<input>", JS_EVAL_TYPE_MODULE);
    JSValue global_obj = JS_GetGlobalObject(ct);
//    JS_SetPropertyStr(ct, global_obj, "remove",
    //                   JS_NewCFunction(ct, o_remove, "remove", 1));
    JS_SetPropertyStr(ct, global_obj, "activity", pushJObject(ct, act));
    // JS_SetPropertyStr(ct,obj,"act",pushJObject(ct,act));
    JS_FreeValue(ct, global_obj);
    if (JS_IsException(cont.eval(str, "<evalScript>", JS_EVAL_TYPE_GLOBAL).v)) {
        ToastText("failed");
        js_std_dump_error(ct);
    }
    js_std_loop(ct);
    js_std_free_handlers(rt);
    env->ReleaseStringUTFChars(str_, str);
}


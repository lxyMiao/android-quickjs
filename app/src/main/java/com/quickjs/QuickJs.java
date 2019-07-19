package com.quickjs;

import android.content.Context;

public class QuickJs {
    static {
        System.loadLibrary("quickjspp");
    }

    public static native boolean open();

    //public static native boolean openJsContext();
    public static native void evalString(Context act, String str);
    //public static native void pushJobject();
}

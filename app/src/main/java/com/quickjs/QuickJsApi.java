package com.quickjs;

import android.util.Log;

import com.quickjs.QuickJs;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class QuickJsApi {
    private final static HashMap<Class<?>, HashMap<String, ArrayList<Method>>> methodCache3 = new HashMap<Class<?>, HashMap<String, ArrayList<Method>>>();
    public final static HashMap<Class<?>, Method[]> methodsMap = new HashMap<Class<?>, Method[]>();
    public final static HashMap<String, Method[]> methodCache = new HashMap<String, Method[]>();
    private static HashMap<String, Method> stringMethodCache = new HashMap<>();
    private static HashMap<String, Method> integerMethodCache = new HashMap<>();
    private static HashMap<String, Method> doubleMethodCache = new HashMap<>();
    private static HashMap<String, Method> boolMethodCache = new HashMap<>();
    private static HashMap<String, Method> voidMethodCache = new HashMap<>();

    private QuickJsApi() throws Exception {

    }

    public static void clearCaches() {
        methodCache.clear();
        methodsMap.clear();

        stringMethodCache.clear();
        integerMethodCache.clear();
        doubleMethodCache.clear();
        boolMethodCache.clear();
        voidMethodCache.clear();
    }

    public static Object compareTypes(Class<?> parameter, int type, Object userObj) throws Exception {
        boolean okType = true;
        Object obj = null;
        if (userObj == null) {
            return null;
        } else if (parameter.isPrimitive()) {
            Class<?> clazz = userObj.getClass();
            if (parameter == byte.class && userObj instanceof Byte) {
                obj = userObj;
            } else if (parameter == short.class && userObj instanceof Short) {
                obj = userObj;
            } else if (parameter == int.class && userObj instanceof Integer) {
                obj = userObj;
            } else if (parameter == long.class && userObj instanceof Long) {
                obj = userObj;
            } else if (parameter == float.class && userObj instanceof Float) {
                obj = userObj;
            } else if (parameter == double.class && userObj instanceof Double) {
                obj = userObj;
            } else if (parameter == char.class && userObj instanceof Character) {
                obj = userObj;
            }
        }
        if (obj == null) {
            if (parameter.isAssignableFrom(userObj.getClass())) {
                obj = userObj;
            } else {
                okType = false;
            }
        }
        if (!okType || obj == null) {
            throw new Exception("Invalid Parameter.");
        }
        return obj;
    }

    private static Boolean toType(Class parameters[], Object[] arr) {
        for (int i = 0; i < arr.length; i++) {
            Class parameter = parameters[i];
            Object mobj = arr[i];
            Object obj = null;
            Class<?> clazz = mobj.getClass();
            if (parameter == byte.class && mobj instanceof Byte) {
                obj = ((Byte) mobj).byteValue();
            } else if (parameter == short.class && mobj instanceof Short) {
                obj = ((Short) mobj).shortValue();
            } else if (parameter == int.class && mobj instanceof Integer) {
                obj = ((Integer) mobj).intValue();
                Log.d("quickjs", String.valueOf(obj));
            } else if (parameter == long.class && mobj instanceof Long) {
                obj = ((Long) mobj).longValue();
            } else if (parameter == float.class && mobj instanceof Float) {
                obj = ((Float) mobj).floatValue();
            } else if (parameter == double.class && mobj instanceof Double) {
                obj = ((Double) mobj).doubleValue();
            } else if (parameter == char.class && mobj instanceof Character) {
                obj = ((Character) mobj).charValue();
            } else if (parameter == boolean.class && mobj instanceof Boolean) {
                obj = ((Boolean) mobj).booleanValue();
            } else if (clazz.isAssignableFrom(parameter)) {
                obj = mobj;
            } else {
                return false;
            }
            arr[i] = obj;
        }
        return true;
    }

    /* private static boolean CmpType(Class[] type, Object... args) {
         for (int i = 0; i < type.length; i++) {
             if (type[i].isAssignableFrom(args[i].getClass()))
                 return true;
         }
         return false;
     }
 */
    public static Object objectIndex(Object obj, String searchName, int type)
            throws Exception {
        int ret = 0;
        if (type == 0)
            if (checkMethod(obj, searchName) != 0)
                return 2;

        if (type == 0 || type == 1 || type == 5)
            if ((ret = checkField(obj, searchName)) != 0)
                return ret;

        if (type == 0 || type == 3)
            if (checkClass(obj, searchName) != 0)
                return 3;

            /*if ((type == 0 || type == 6) && obj instanceof LuaMetaTable) {
                Object res = ((LuaMetaTable) obj).__index(searchName);
                L.pushObjectValue(res);
                return 6;
            }*/

        return 0;

    }


    private static Object mInvoke(Class clz, String methodn, Object obj, Object... args) throws InvocationTargetException, IllegalAccessException {
        Method inm = null;
        Log.d("quickjs", methodn);
        Method[] methods = clz.getMethods();
        for (Method m : methods) {
            if (methodn.equals(m.getName()) && (m.getParameterTypes().length == args.length)) {
                if (toType(m.getParameterTypes(), args) || args.length == 0) {
                    Log.d("qucikjs", "method find");
                    inm = m;
                    break;
                }
            }
        }
        if (inm != null) {
            inm.setAccessible(true);
            return inm.invoke(obj, args);
        }
        Log.d("quickjs", "is null");
        return null;
    }

    public static Object newJInstance(Class clz, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor mcon = null;
        Constructor[] cons = clz.getConstructors();
        for (Constructor con : cons) {
            if (con.getParameterTypes().length == args.length) {
                if (toType(con.getParameterTypes(), args) || args.length == 0) {
                    mcon = con;
                    break;
                }
            }
        }
        if (mcon != null) {
            mcon.setAccessible(true);
            return mcon.newInstance(args);
        }
        return null;
    }

    public static Object callMethod(Object obj, String methodn, Object... args) throws InvocationTargetException, IllegalAccessException {
        if (obj instanceof Class) {
            return mInvoke((Class) obj, methodn, null, args);
        }
        return mInvoke(obj.getClass(), methodn, obj, args);
    }

    /* public static Object callMethod(Object obj, String cacheName, Object... arg)
             throws Exception {

         StringBuilder msgBuilder = new StringBuilder();
         Method method = null;

         if (method != null) {
             Object ret;
             try {
                 if (!Modifier.isPublic(method.getModifiers()))
                     method.setAccessible(true);

                 ret = method.invoke(obj, arg);
             } catch (Exception e) {
                 msgBuilder.append("  at ").append(method).append("\n  -> ").append((e.getCause() != null) ? e.getCause() : e).append("\n");
                 throw new Exception("Invalid method call.\n" + msgBuilder.toString());
             }

             // Void function returns null
             if (ret == null && method.getReturnType().equals(Void.TYPE))
                 return null;

             // push result
             return ret;
         }

         Object objs = new Object[arg.length];
         Method[] methods = methodCache.get(cacheName);
         // gets method and arguments
         for (Method m : methods) {

             Class[] parameters = m.getParameterTypes();


             boolean okMethod = true;


             if (okMethod) {
                 method = m;
                 Object ret;
                 try {
                     if (!Modifier.isPublic(method.getModifiers()))
                         method.setAccessible(true);

                     ret = method.invoke(obj, objs);
                 } catch (Exception e) {
                     msgBuilder.append("  at ").append(method).append("\n  -> ").append((e.getCause() != null) ? e.getCause() : e).append("\n");
                     continue;
                 }

                 // Void function returns null
                 if (ret == null && method.getReturnType().equals(Void.TYPE))
                     return 0;

                 // push result
                 return ret;
             }
         }

         if (msgBuilder.length() > 0) {
             throw new Exception("Invalid method call.\n" + msgBuilder.toString());
         }
         // If method is null means there isn't one receiving the given arguments
         for (Method m : methods) {
             msgBuilder.append(m.toString());
             msgBuilder.append("\n");
         }
         throw new Exception("Invalid method call. Invalid Parameters.\n" + msgBuilder.toString());


     }

 */
    /* public static int objectNewIndex(long luaState, Object obj, String searchName, int type)
             throws LuaException {
         LuaState L = LuaStateFactory.getExistingState(luaState);
         synchronized (L) {
             int res;
             if (type == 0 || type == 1) {
                 res = setFieldValue(L, obj, searchName);
                 if (res != 0)
                     return 1;
             }

             if (type == 0 || type == 2) {
                 res = javaSetter(L, obj, searchName);
                 if (res != 0)
                     return 2;
             }
             if (type == 0 || type == 3) {
                 if (obj instanceof LuaMetaTable) {
                     ((LuaMetaTable) obj).__newIndex(searchName, L.toJavaObject(-1));
                     return 3;
                 }
             }
             return 0;
         }
     }
 */
    public static int setFieldValue(Object obj, String fieldName, Object... arg) throws Exception {
        Field field = null;
        Class objClass;
        boolean isClass = false;

        if (obj == null)
            return 0;

        if (obj instanceof Class) {
            objClass = (Class) obj;
            isClass = true;
        } else {
            objClass = obj.getClass();
        }

        try {
            field = objClass.getField(fieldName);
        } catch (NoSuchFieldException e) {
            return 0;
        }

        if (field == null)
            return 0;
        if (isClass && !Modifier.isStatic(field.getModifiers()))
            return 0;
        Class type = field.getType();
        try {
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);

            field.set(obj, arg);
        } catch (Exception e) {
            throw new Exception(e);
        }

        return 1;

    }

    private static String argError(String name, int idx, Class type) throws Exception {

        throw new Exception("bad argument to '" + name + "' (" + type.getName() + " expected, got " + " value)");

    }

    /*
        private static String typeName(LuaState L, int idx) throws LuaException {
            if (L.isObject(idx)) {
                return L.getObjectFromUserdata(idx).getClass().getName();
            }
            switch (L.type(idx)) {
                case LuaState.LUA_TSTRING:
                    return "string";
                case LuaState.LUA_TNUMBER:
                    return "number";
                case LuaState.LUA_TBOOLEAN:
                    return "boolean";
                case LuaState.LUA_TFUNCTION:
                    return "function";
                case LuaState.LUA_TTABLE:
                    return "table";
                case LuaState.LUA_TTHREAD:
                    return "thread";
                case LuaState.LUA_TLIGHTUSERDATA:
                case LuaState.LUA_TUSERDATA:
                    return "userdata";
            }
            return "unkown";
        }

        *//*
    public static int setArrayValue(long luaState, Object obj, int index) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            if (obj.getClass().isArray()) {
                Class<?> type = obj.getClass().getComponentType();
                try {
                    Object value = compareTypes(L, type, 3);
                    Array.set(obj, index, value);
                } catch (LuaException e) {
                    argError(L, obj.getClass().getName() + " [" + index + "]", 3, type);
                }
            } else if (obj instanceof List) {
                ((List<Object>) obj).set(index, L.toJavaObject(3));
            } else if (obj instanceof Map) {
                ((Map<Long, Object>) obj).put((long) index, L.toJavaObject(3));
            } else {
                throw new LuaException("can not set " + obj.getClass().getName() + " value: " + L.toJavaObject(3) + " in " + index);
            }
            return 0;
        }
    }

    public static int setArrayValue(LuaState L, Object obj, int index) throws LuaException {

        synchronized (L) {
            if (obj.getClass().isArray()) {
                Class<?> type = obj.getClass().getComponentType();
                try {
                    Object value = compareTypes(L, type, -1);
                    Array.set(obj, index, value);
                } catch (LuaException e) {
                    argError(L, obj.getClass().getName() + " [" + index + "]", 3, type);
                }
            } else if (obj instanceof List) {
                ((List<Object>) obj).set(index, L.toJavaObject(-1));
            } else if (obj instanceof Map) {
                ((Map<Long, Object>) obj).put((long) index, L.toJavaObject(-1));
            } else {
                throw new LuaException("can not set " + obj.getClass().getName() + " value: " + L.toJavaObject(-1) + " in " + index);
            }
            return 0;
        }
    }

    public static int getArrayValue(long luaState, Object obj, int index) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Object ret = null;
            if (obj.getClass().isArray()) {
                ret = Array.get(obj, index);
            } else if (obj instanceof List) {
                ret = ((List) obj).get(index);
            } else if (obj instanceof Map) {
                ret = ((Map) obj).get((long) index);
            } else {
                throw new LuaException("can not get " + obj.getClass().getName() + " value in " + index);
            }
            L.pushObjectValue(ret);
            return 1;
        }
    }

    public static int asTable(long luaState, Object obj) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {

            try {
                L.newTable();
                if (obj.getClass().isArray()) {
                    int n = Array.getLength(obj);
                    for (int i = 0; i <= n - 1; i++) {
                        L.pushObjectValue(Array.get(obj, i));
                        L.rawSetI(-2, i + 1);
                    }
                } else if (obj instanceof Collection) {
                    Collection list = (Collection) obj;
                    int i = 1;
                    for (Object v : list) {
                        L.pushObjectValue(v);
                        L.rawSetI(-2, i++);
                    }
                } else if (obj instanceof Map) {
                    Map map = (Map) obj;
                    for (Object o : map.entrySet()) {
                        Map.Entry entry = (Map.Entry) o;
                        L.pushObjectValue(entry.getKey());
                        L.pushObjectValue(entry.getValue());
                        L.setTable(-3);
                    }
                }
                L.pushValue(-1);
                return 1;
            } catch (Exception e) {
                throw new LuaException("can not astable: " + e.getMessage());
            }

        }
    }

    public static int newArray(long luaState, Class<?> clazz, int size) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);
        synchronized (L) {
            try {
                Object obj = Array.newInstance(clazz, size);
                L.pushJavaObject(obj);
            } catch (Exception e) {
                throw new LuaException("can not create a array: " + e.getMessage());
            }
            return 1;
        }
    }

    public static int newArray(long luaState, Class<?> clazz) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);
        synchronized (L) {
            try {
                int top = L.getTop();
                int[] dimensions = new int[top - 1];
                for (int i = 0; i < top - 1; i++) {
                    dimensions[i] = (int) L.toInteger(i + 2);
                }
                Object obj = Array.newInstance(clazz, dimensions);
                L.pushJavaObject(obj);
            } catch (Exception e) {
                throw new LuaException("can not create a array: " + e.getMessage());
            }
            return 1;
        }
    }
*/
    public static Class javaBindClass(String className) throws Exception {
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (Exception e) {
            switch (className) {
                case "boolean":
                    clazz = Boolean.TYPE;
                    break;
                case "byte":
                    clazz = Byte.TYPE;
                    break;
                case "char":
                    clazz = Character.TYPE;
                    break;
                case "short":
                    clazz = Short.TYPE;
                    break;
                case "int":
                    clazz = Integer.TYPE;
                    break;
                case "long":
                    clazz = Long.TYPE;
                    break;
                case "float":
                    clazz = Float.TYPE;
                    break;
                case "double":
                    clazz = Double.TYPE;
                    break;
                default:
                    throw new Exception("Class not found: " + className);
            }
        }
        return clazz;
    }

    public static Object javaNewInstance(String className, Object... args) throws Exception {
        Class clazz;
        clazz = javaBindClass(className);
        if (clazz.isPrimitive())
            return clazz;
        else
            return getObjInstance(clazz, args);

    }


   /* public static int javaNew( Class<?> clazz) throws LuaException {
            if (clazz.isPrimitive()) {
                int top = L.getTop();
                for (int i = 2; i <= top; i++) {
                    toPrimitive(L, clazz, i);
                }
                return top - 1;
            } else {
                return getObjInstance(L, clazz);
            }

    }*/

   /* public static int javaCreate(long luaState, Class<?> clazz) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            if (clazz.isPrimitive()) {
                return createArray(L, clazz);
            } else if (List.class.isAssignableFrom(clazz)) {
                return createList(L, clazz);
            } else if (Map.class.isAssignableFrom(clazz)) {
                return createMap(L, clazz);
            } else if (clazz.isInterface()) {
                return createProxyObject(L, clazz);
            } else if ((clazz.getModifiers()&Modifier.ABSTRACT)!=0) {
                return createAbstractProxy(L, clazz);
            }else {
                if (L.objLen(-1) == 0)
                    return createArray(L, clazz);
                else if (clazz.isAssignableFrom(new LuaTable(L, -1).get(1).getClass()))
                    return createArray(L, clazz);
                else
                    return getObjInstance(L, clazz);

            }
        }

    }

    private static int createAbstractProxy(LuaState L, Class<?> clazz) {
        Class<?> cls = new LuaEnhancer(clazz).create(new MethodFilter() {
            @Override
            public boolean filter(Method method, String name) {
                if ((method.getModifiers() & Modifier.ABSTRACT) == 0)
                    return true;
                return false;
            }
        });
        try {
            EnhancerInterface obj = (EnhancerInterface) cls.newInstance();
            obj.setMethodInterceptor_Enhancer(new LuaMethodInterceptor(L.getLuaObject(-1)));
            L.pushJavaObject(obj);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }*/
/*
    public static int objectCall(long luaState, Object obj) throws Exception {
            if (obj instanceof LuaMetaTable) {
                int n = L.getTop();
                Object[] args = new Object[n - 1];
                for (int i = 2; i <= n; i++) {
                    args[i - 2] = L.toJavaObject(i);
                }
                Object ret = ((LuaMetaTable) obj).__call(args);
                L.pushObjectValue(ret);
                return 1;
            } else {
                if (L.isTable(2)) {
                    L.pushNil();
                    if (obj instanceof List) {
                        List list = (List) obj;
                        while (L.next(2) != 0) {
                            list.add(L.toJavaObject(-1));
                            L.pop(1);
                        }
                    } else {
                        while (L.next(2) != 0) {
                            if (L.isNumber(-2))
                                setArrayValue(L, obj, (int) L.toInteger(-2));
                            else
                                javaSetter(L, obj, L.toString(-2));
                            L.pop(1);
                        }
                    }
                    L.setTop(1);
                    return 1;
                } else {
                    return 0;
                }
            }
    }

  *//*public static int createProxy(long luaState, String implem)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);
        synchronized (L) {
            return createProxyObject(L, implem);
        }
    }

    public static int createArray(long luaState, String className)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);
        synchronized (L) {
            Class type = javaBindClass(className);
            return createArray(L, type);
        }
    }
*/

   /* public static int javaLoadLib(long luaState, String className, String methodName)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new LuaException(e);
            }

            try {
                Method mt = clazz.getMethod(methodName, LuaState.class);
                Object obj = mt.invoke(null, L);

                if (obj != null && obj instanceof Integer) {
                    return (Integer) obj;
                } else
                    return 0;
            } catch (Exception e) {
                throw new LuaException("Error on calling method. Library could not be loaded. " + e.getMessage());
            }
        }
    }*/

    public static String javaToString(Object obj) throws Exception {

        if (obj == null)
            return ("null");
        else
            return obj.toString();

    }

    /*public static void javaGc(Object obj) throws LuaException {
        Log.i("javaGc: ", obj + "");
        if (obj == null)
            return;
        try {
            if (obj instanceof LuaGcable)
                ((LuaGcable) obj).gc();
            else if (obj instanceof Bitmap)
                ((Bitmap) obj).recycle();
            else if (obj instanceof BitmapDrawable)
                ((BitmapDrawable) obj).getBitmap().recycle();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static String javaGetType(Object obj) throws Exception {

        if (obj == null)
            return ("null");
        else
            return (obj.getClass().getName());

    }

    public static Boolean javaEquals(Object obj, Object obj2) throws Exception {

        boolean eq = obj.equals(obj2);
        return eq;

    }

    public static int javaObjectLength(long luaState, Object obj) throws Exception {
        int ret;
        try {
            if (obj instanceof CharSequence)
                ret = ((CharSequence) obj).length();
            else if (obj instanceof Collection)
                ret = ((Collection) obj).size();
            else if (obj instanceof Map)
                ret = ((Map) obj).size();
            else
                ret = Array.getLength(obj);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return ret;
    }

    private static Object getObjInstance(Class<?> clazz, Object... arg) throws Exception {

        if (arg.length == 0) {
            try {
                Object ret = clazz.newInstance();
                return (ret);
            } catch (Exception e) {
            }
        }
        Constructor[] constructors = clazz.getConstructors();
        Constructor constructor = null;

        StringBuilder msgBuilder = new StringBuilder();
        // gets method and arguments
        for (Constructor c : constructors) {
            Class<?>[] parameters = c.getParameterTypes();
            if (parameters.length != arg.length)
                continue;

            boolean okConstructor = true;


            if (okConstructor) {
                constructor = c;
                Object ret;
                try {
                    ret = constructor.newInstance(arg);
                } catch (Exception e) {
                    msgBuilder.append("  at ").append(constructor).append("\n  -> ").append((e.getCause() != null) ? e.getCause() : e).append("\n");
                    continue;
                }
                return ret;
                //break;
            }
        }

        if (msgBuilder.length() > 0) {
            throw new Exception("Invalid constructor method call.\n" + msgBuilder.toString());
        }

        for (Constructor c : constructors) {
            msgBuilder.append(c.toString());
            msgBuilder.append("\n");
        }

        throw new Exception("Invalid constructor method call. Invalid Parameters.\n" + msgBuilder.toString());

            /*// If method is null means there isn't one receiving the given arguments
            if (constructor == null) {
                StringBuilder msgBuilder = new StringBuilder();
                for (Constructor c : constructors) {
                    msgBuilder.append(c.toString());
                    msgBuilder.append("\n");
                }
                throw new LuaException("Invalid constructor method call. Invalid Parameters.\n" + msgBuilder.toString());
            }

            Object ret;
            try {
                ret = constructor.newInstance(objs);
            } catch (Exception e) {
                throw new LuaException(e);
            }

            if (ret == null) {
                throw new LuaException("Couldn't instantiate java Object");
            }
            L.pushJavaObject(ret);
            return 1;*/

    }


    public static ArrayList<Method> getMethod(Class<?> clazz, String methodName, boolean isClass) {
        //String className = clazz.getName();
        HashMap<String, ArrayList<Method>> cList = methodCache3.get(clazz);
        if (cList == null) {
            cList = new HashMap<>();
            methodCache3.put(clazz, cList);
        }

        ArrayList<Method> mlist = cList.get(methodName);
        if (mlist == null) {
            Method[] methods = methodsMap.get(clazz);
            if (methods == null) {
                methods = clazz.getMethods();
                methodsMap.put(clazz, methods);
            }
            for (Method method : methods) {
                String name = method.getName();
                ArrayList<Method> list = cList.get(name);
                if (list == null) {
                    list = new ArrayList<Method>();
                    cList.put(name, list);
                }
                list.add(method);
            }
            mlist = cList.get(methodName);
        }

        if (mlist == null) {
            mlist = new ArrayList<Method>();
        }
        if (isClass) {
            ArrayList<Method> slist = new ArrayList<Method>();
            for (Method m : mlist) {
                if (Modifier.isStatic(m.getModifiers()))
                    slist.add(m);
            }

            if (slist.isEmpty()) {
                slist = getMethod(Class.class, methodName, false);
            }
            return slist;
        }
        return mlist;
    }


    /**
     * Checks if there is a field on the obj with the given name
     *
     * @param obj       object to be inspected
     * @param fieldName name of the field to be inpected
     * @return number of returned objects
     */
    public static int checkField(Object obj, String fieldName)
            throws Exception {
        Field field = null;
        Class objClass;
        boolean isClass = false;

        if (obj instanceof Class) {
            objClass = (Class) obj;
            isClass = true;
        } else {
            objClass = obj.getClass();
        }

        try {
            field = objClass.getField(fieldName);
        } catch (NoSuchFieldException e) {
            return 0;
        }

        if (field == null)
            return 0;

        if (isClass && !Modifier.isStatic(field.getModifiers()))
            return 0;

        Object ret = null;
        try {
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            ret = field.get(obj);
        } catch (Exception e) {
            throw new Exception(e);
        }
        if (Modifier.isFinal(field.getModifiers()))
            return 5;
        else
            return 1;

    }


    public static int checkMethod(Object obj, String methodName) throws Exception {
        Class<?> clazz;
        boolean isClass = false;
        if (obj instanceof Class) {
            clazz = (Class<?>) obj;
            isClass = true;
        } else {
            clazz = obj.getClass();
        }
        //String className=clazz.getName();;
        String cacheName = clazz.getName();
        Method[] mlist = methodCache.get(methodName);
        if (mlist == null) {
            ArrayList<Method> list = getMethod(clazz, methodName, isClass);
            mlist = new Method[list.size()];
            list.toArray(mlist);
            methodCache.put(cacheName, mlist);
        }
        if (mlist.length == 0)
            return 0;
        return 2;

    }

    public static int checkClass(Object obj, String className) throws Exception {
        Class clazz;

        if (obj instanceof Class) {
            clazz = (Class) obj;
        } else {
            return 0;
        }

        Class[] clazzes = clazz.getClasses();

        for (Class c : clazzes) {
            if (c.getSimpleName().equals(className)) {

                return 3;
            }
        }
        return 0;
    }


}

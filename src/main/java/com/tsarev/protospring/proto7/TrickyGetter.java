package com.tsarev.protospring.proto7;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

public class TrickyGetter {
    static Object doTest(MethodHandle handle) throws Throwable {
        return handle.invokeExact();
    }

    static Object doTest(VarHandle handle, Object instance) throws Throwable {
        return handle.get(instance);
    }

    static <T, R> Function<T, R> doTest2(MethodHandle handle) throws Throwable {
        return (Function<T, R>) handle.invoke();
    }
}

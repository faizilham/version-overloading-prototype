// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: lib
// FILE: lib/lib.kt
package lib

import com.faizilham.prototype.versioning.*

object MyExample {
    @JvmStatic
    @VersionOverloads
    fun myAdd(
        x: Int,
        y: Int = 0,
        @Version("1.2") z: Int = 0,
        @Version("1.3") a1: Int = 0,
        @Version("1.3") a2: Int = 0,
    ): Int = 0
}

//class Simple @VersionOverloads constructor(val x: Int, val y: Int = 0, @Version("1.2") val z : Int = 0) {
//
//}

//data class DataCls(val x: Int, val y: Int = 0, val z: Int = 0)

// FILE: lib/JavaTest.java
package lib;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class JavaTest {
    Class<?> clazz = MyExample.class;
    String[] overloads = new String[] {
        "myAdd(int,int):int syn:true",
        "myAdd(int,int,int):int syn:true",
        "myAdd(int,int,int,int,int):int syn:false"
    };

    public String boxTest() {
        HashSet<String> methods = reflectMethods();

        for (String overload : overloads) {
            if (!methods.contains(overload)) {
                return "Fail: overload not found" + overload;
            }
        }

        return "OK";
    }

    private HashSet<String> reflectMethods() {;
        Method[] methods = clazz.getDeclaredMethods();

        HashSet<String> methodNames = new HashSet<>();

        for (Method method : methods) {
            String methodName = method.getName() +
                    "(" +
                    Arrays.stream(method.getParameters())
                        .map(p -> p.getType().getSimpleName())
            .collect(Collectors.joining(",")) +
                "):" + method.getReturnType().getName() +
                " syn:" + method.isSynthetic();

            methodNames.add(methodName);
        }

        return methodNames;
    }
}

// FILE: lib/main.kt
package lib

fun box() : String {
    return JavaTest().boxTest()
}

// MODULE: m2(lib)
// FILE: m2.kt
package m2

import lib.MyExample.myAdd

fun test() {
    myAdd(1)
    myAdd(1, 1)
}

fun test2() {
    myAdd(1, 1, 1)
    myAdd(1, 1, 1, 1)
}


//@JvmOverloads
//fun overloadExample(a: Int, b: Int = 0) = a + b
//
//fun manualOverload(a: Int, b: Int) = a + b
//fun manualOverload(a: Int) = manualOverload(a, 0)

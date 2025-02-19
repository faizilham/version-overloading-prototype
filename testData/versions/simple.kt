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
    ): Int = x + y + z + a1 + a2
}


//data class DataCls(val x: Int, val y: Int = 0, val z: Int = 0)

// FILE: lib/JavaTest.java
package lib;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class JavaTest {
    Class<?> clazz = MyExample.class;

    public ArrayList<Integer> invokeMethods() throws Exception {
        Method method = null;

        ArrayList<Integer> results = new ArrayList<Integer>();

        method = clazz.getDeclaredMethod("myAdd", int.class, int.class);
        results.add((int) method.invoke(null, 1, 1));

        method = clazz.getDeclaredMethod("myAdd", int.class, int.class, int.class);
        results.add((int) method.invoke(null, 1, 1, 1));

        method = clazz.getDeclaredMethod("myAdd", int.class, int.class, int.class, int.class, int.class);
        results.add((int) method.invoke(null, 1, 1, 1, 1, 1));

        return results;
    }

    public HashSet<String> reflectMethods() {;
        Method[] methods = clazz.getDeclaredMethods();

        HashSet<String> signatures = new HashSet<>();

        for (Method method : methods) {
            String signature =
                method.getName() + "(" +
                Arrays.stream(method.getParameterTypes())
                    .map(p -> p.getSimpleName())
                    .collect(Collectors.joining(",")) +
                "):" + method.getReturnType().getName() +
                " syn:" + method.isSynthetic();

            signatures.add(signature);
        }

        return signatures;
    }
}

// FILE: lib/main.kt
package lib

import kotlin.test.*

fun box() : String {
    val javaTest = JavaTest()
    val methods = javaTest.reflectMethods()

    val overloads = listOf(
        "myAdd(int,int):int syn:true",
        "myAdd(int,int,int):int syn:true",
        "myAdd(int,int,int,int,int):int syn:false"
    )

    val overloadCounts = mapOf("myAdd" to 3)

    for (overload in overloads) {
        if (!methods.contains(overload)) return "Fail: overload not found $overload"
    }

    for ((name, expected) in overloadCounts) {
        val pattern = "$name("
        val actual = methods.count { it.startsWith(pattern) }
        if (actual != expected) return "Fail: overload $name count does not match, expected $expected, actual $actual"
    }

    assertContentEquals(listOf(2, 3, 5), javaTest.invokeMethods())

    return "OK"
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

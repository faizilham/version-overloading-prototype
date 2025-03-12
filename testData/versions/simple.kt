// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: lib
// FILE: lib/lib.kt
package lib

import com.faizilham.prototype.versioning.*

const val VERSION_1_2 = "1.2"
const val VERSION_1_3 = "1.3"

object MyExample {
    @JvmStatic
    fun myAdd(
        x: Int,
        y: Int = 0,
        @IntroducedAt("1.2") z: Int = 0,
        @IntroducedAt("1.3") a1: Int = 0,
        @IntroducedAt("1.3") a2: Int = 0,
    ): Int = x + y + z + a1 + a2

    @JvmStatic
    fun middle(
        x: Float,
        @IntroducedAt(VERSION_1_2) x1: Float = 0.0f,
        y: Int = 0,
        @IntroducedAt(VERSION_1_3) y1: Int = 0,
        @IntroducedAt(VERSION_1_2) z: Float = 0.0f,
    ): Float = x + x1 + y + y1 + z
}


//data class DataCls(val x: Int, val y: Int = 0, val z: Int = 0)

// FILE: lib/JavaTest.java
package lib;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;
import kotlin.Pair;

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

    public HashSet<Pair<String, Boolean>> reflectMethods() {;
        Method[] methods = clazz.getDeclaredMethods();

        HashSet<Pair<String, Boolean>> signatures = new HashSet<>();

        for (Method method : methods) {
            signatures.add(new Pair(method.toGenericString(), method.isSynthetic()));
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
        Pair("public static final int lib.MyExample.myAdd(int,int)", true),
        Pair("public static final int lib.MyExample.myAdd(int,int,int)", true),
        Pair("public static final int lib.MyExample.myAdd(int,int,int,int,int)", false),

        Pair("public static final float lib.MyExample.middle(float,int)", true),
        Pair("public static final float lib.MyExample.middle(float,float,int,float)", true),
        Pair("public static final float lib.MyExample.middle(float,float,int,int,float)", false)
    )

    val overloadCounts = mapOf(
        "myAdd" to 3,
        "middle" to 3
    )

    for (overload in overloads) {
        if (!methods.contains(overload)) {
            println("Overload check fail")
            for ((methodName, syn) in methods) {
                println("$methodName, syn: $syn")
            }
            return "Fail: overload not found $overload"
        }
    }

    for ((name, expected) in overloadCounts) {
        val pattern = "$name("
        val actual = methods.count { (sign, _) -> sign.contains(pattern) }
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

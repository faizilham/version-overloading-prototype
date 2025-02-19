// FILE: trail_m1.kt
import com.faizilham.prototype.versioning.*

object TrailingExample {
    @JvmStatic
    @VersionOverloads
    fun withlam(
        x: Int,
        y: Int = 0,
        @Version("1.2") z: Int = 0,
        @Version("1.3") a1: Int = 0,
        @Version("1.4") a2: Int = 0,
        f: (Int) -> Int
    ): Int = f(x)

    @JvmStatic
    @VersionOverloads
    fun withDefLam(
        x: Int,
        y: Int = 0,
        @Version("1.2") z: Int = 0,
        @Version("1.3") a: Int = 0,
        f: (Int) -> Int = { it }
    ): Int = f(x)
}

// FILE: trail_m2.kt

import TrailingExample.withlam
import TrailingExample.withDefLam

fun test() {
    withlam(1, 1) { it }
    withlam(1, 1, 2) { it }
    withlam(1, 1, 2, 3) { it }
    withlam(1, 1, 2, 3, 4) { it }

}

fun test2() {
    withDefLam(1, 1)
    withDefLam(1, 1, 2)
    withDefLam(1, 1, 2) { 0 }
    withDefLam(1, 1, 2, 3)
}


// FILE: JavaTest.java
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class JavaTest {
    Class<?> clazz = TrailingExample.class;

    public HashSet<String> reflectMethods() {;
        Method[] methods = clazz.getDeclaredMethods();

        HashSet<String> signatures = new HashSet<>();

        for (Method method : methods) {
            String signature =
                method.getName() + "(" +
                Arrays.stream(method.getParameters())
                    .map(p -> p.getType().getSimpleName())
                    .collect(Collectors.joining(",")) +
                "):" + method.getReturnType().getName() +
                " syn:" + method.isSynthetic();

            signatures.add(signature);
        }

        return signatures;
    }
}

// FILE: main.kt

fun box() : String {
    val methods = JavaTest().reflectMethods()

    val overloads = listOf(
        "withlam(int,int,Function1):int syn:true",
        "withlam(int,int,int,Function1):int syn:true",
        "withlam(int,int,int,int,Function1):int syn:true",
        "withlam(int,int,int,int,int,Function1):int syn:false",

        "withDefLam(int,int,Function1):int syn:true",
        "withDefLam(int,int,int,Function1):int syn:true",
        "withDefLam(int,int,int,int,Function1):int syn:false",
    )

    val overloadCounts = mapOf(
        "withlam" to 4,
        "withDefLam" to 3
    )

    for (overload in overloads) {
        if (!methods.contains(overload)) return "Fail: overload not found $overload"
    }

    for ((name, expected) in overloadCounts) {
        val pattern = "$name("
        val actual = methods.count { it.startsWith(pattern) }
        if (actual != expected) return "Fail: overload $name count does not match, expected $expected, actual $actual"
    }

    return "OK"
}
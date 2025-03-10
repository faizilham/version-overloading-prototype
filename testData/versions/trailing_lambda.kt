// FILE: trail_m1.kt
import com.faizilham.prototype.versioning.*

object TrailingExample {
    @JvmStatic
    @VersionOverloads
    fun withlam(
        x: Int,
        y: Int = 0,
        @IntroducedAt("1.2") z: Int = 0,
        @IntroducedAt("1.3") a1: Int = 0,
        @IntroducedAt("1.4") a2: Int = 0,
        f: (Int) -> Int
    ): Int = f(x + y + z + a1 + a2)

    @JvmStatic
    @VersionOverloads
    fun withDefLam(
        x: Int,
        y: Int = 0,
        @IntroducedAt("1.2") z: Int = 0,
        @IntroducedAt("1.3") a: Int = 0,
        f: (Int) -> Int = { it }
    ): Int = f(x + y + z + a)

    @JvmStatic
    @VersionOverloads
    fun middle(
        x: Float,
        @IntroducedAt("1.2") x1: Float = 0.0f,
        y: Int = 0,
        @IntroducedAt("1.3") y1: Int = 0,
        @IntroducedAt("1.2") z: Float = 0.0f,
        f: (Float) -> Float
    ): Float = f(x + x1 + y + y1 + z)
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
import kotlin.Pair;

public class JavaTest {
    Class<?> clazz = TrailingExample.class;

    public HashSet<Pair<String, Boolean>> reflectMethods() {;
        Method[] methods = clazz.getDeclaredMethods();

        HashSet<Pair<String, Boolean>> signatures = new HashSet<>();

        for (Method method : methods) {
            signatures.add(new Pair(method.toGenericString(), method.isSynthetic()));
        }

        return signatures;
    }
}

// FILE: main.kt

fun box() : String {
    val methods = JavaTest().reflectMethods()

    val overloads = listOf(
        Pair("public static final int TrailingExample.withDefLam(int,int,int,kotlin.jvm.functions.Function1)", true),
        Pair("public static final int TrailingExample.withDefLam(int,int,kotlin.jvm.functions.Function1)", true),
        Pair("public static final int TrailingExample.withDefLam(int,int,int,int,kotlin.jvm.functions.Function1<? super java.lang.Integer, java.lang.Integer>)", false),

        Pair("public static final int TrailingExample.withlam(int,int,kotlin.jvm.functions.Function1)", true),
        Pair("public static final int TrailingExample.withlam(int,int,int,kotlin.jvm.functions.Function1)", true),
        Pair("public static final int TrailingExample.withlam(int,int,int,int,kotlin.jvm.functions.Function1)", true),
        Pair("public static final int TrailingExample.withlam(int,int,int,int,int,kotlin.jvm.functions.Function1<? super java.lang.Integer, java.lang.Integer>)", false),

        Pair("public static final float TrailingExample.middle(float,int,kotlin.jvm.functions.Function1)", true),
        Pair("public static final float TrailingExample.middle(float,float,int,float,kotlin.jvm.functions.Function1)", true),
        Pair("public static final float TrailingExample.middle(float,float,int,int,float,kotlin.jvm.functions.Function1<? super java.lang.Float, java.lang.Float>)", false),
    )

    val overloadCounts = mapOf(
        "withlam" to 4,
        "withDefLam" to 3,
        "middle" to 3,
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

    return "OK"
}
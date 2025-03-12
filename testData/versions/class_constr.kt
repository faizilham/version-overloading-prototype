// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: m1.kt
import com.faizilham.prototype.versioning.*

class Simple constructor(
    val x: Int,
    @IntroducedAt("1.1") val y: Int = 0,
    @IntroducedAt("1.2") val z1 : Long = 0,
    @IntroducedAt("1.2") val z2 : Int = 0
    ) {

    fun sum() = x + y + z1 + z2
}

// FILE: JavaTest.java
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import kotlin.Pair;

public class JavaTest {
    Class<?> clazz = Simple.class;

    public HashSet<Pair<String, Boolean>> reflectConstructors() {;
        Constructor[] constrs = clazz.getDeclaredConstructors();

        HashSet<Pair<String, Boolean>> signatures = new HashSet<>();

        for (Constructor constr : constrs) {
            signatures.add(new Pair(constr.toGenericString(), constr.isSynthetic()));
        }

        return signatures;
    }
}

// FILE: main.kt

fun box() : String {
    val constrs = JavaTest().reflectConstructors()

    val overloads = listOf(
        Pair("public Simple(int)", true),
        Pair("public Simple(int,int)", true),
        Pair("public Simple(int,int,long,int)", false)
    )

    val overloadCounts = mapOf(
        "Simple" to 3
    )

    for (overload in overloads) {
        if (!constrs.contains(overload)) return "Fail: overload not found $overload"
    }

    for ((name, expected) in overloadCounts) {
        // NOTE: any constructors with DefaultConstructorMarker are not counted
        val pattern = "$name("
        val actual = constrs.count {(sign, _) -> sign.contains(pattern) && !sign.contains("DefaultConstructorMarker") }
        if (actual != expected) return "Fail: overload $name count does not match, expected $expected, actual $actual"
    }

    return "OK"
}
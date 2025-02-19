// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: m1.kt
import com.faizilham.prototype.versioning.*

class Simple @VersionOverloads constructor(
    val x: Int,
    @Version("1.1") val y: Int = 0,
    @Version("1.2") val z1 : Long = 0,
    @Version("1.2") val z2 : Int = 0
    ) {

    fun sum() = x + y + z1 + z2
}

// FILE: JavaTest.java
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class JavaTest {
    Class<?> clazz = Simple.class;

    public HashSet<String> reflectConstructors() {;
        Constructor[] constrs = clazz.getDeclaredConstructors();

        HashSet<String> signatures = new HashSet<>();

        for (Constructor constr : constrs) {
            String signature =
                constr.getName() + "(" +
                    Arrays.stream(constr.getParameterTypes())
                        .map(p -> p.getSimpleName())
                        .collect(Collectors.joining(",")) +
                ") syn:" + constr.isSynthetic();

            signatures.add(signature);
        }

        return signatures;
    }
}

// FILE: main.kt

fun box() : String {
    val constrs = JavaTest().reflectConstructors()

    val overloads = listOf<String>(
        "Simple(int) syn:true",
        "Simple(int,int) syn:true",
        "Simple(int,int,long,int) syn:false",
    )

    val overloadCounts = mapOf<String, Int>(
        "Simple" to 3
    )

    for (overload in overloads) {
        if (!constrs.contains(overload)) return "Fail: overload not found $overload"
    }

    for ((name, expected) in overloadCounts) {
        // NOTE: any constructors with DefaultConstructorMarker are not counted
        val pattern = "$name("
        val actual = constrs.count { it.startsWith(pattern) && !it.contains("DefaultConstructorMarker") }
        if (actual != expected) return "Fail: overload $name count does not match, expected $expected, actual $actual"
    }

    return "OK"
}
// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: m1.kt
import com.faizilham.prototype.versioning.*

data class Data(
    val x: Int,
    @IntroducedAt("1.2") val x1 : Long = 0,
    @IntroducedAt("1.1") val y: Int = 0,
    @IntroducedAt("1.2") val y2 : Int = 0,
    @IntroducedAt("1.3") val z : Int = 0
)


// FILE: JavaTest.java
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import kotlin.Pair;

public class JavaTest {
    Class<?> clazz = Data.class;

    public HashSet<Pair<String, Boolean>> reflectConstructors() {;
        Constructor[] constrs = clazz.getDeclaredConstructors();

        HashSet<Pair<String, Boolean>> signatures = new HashSet<>();

        for (Constructor constr : constrs) {
            signatures.add(new Pair(constr.toGenericString(), constr.isSynthetic()));
        }

        return signatures;
    }

    public HashSet<Pair<String, Boolean>> reflectCopies() {;
        Method[] methods = clazz.getDeclaredMethods();

        HashSet<Pair<String, Boolean>> signatures = new HashSet<>();

        for (Method method : methods) {
            if (method.getName() != "copy") continue;
            signatures.add(new Pair(method.toGenericString(), method.isSynthetic()));
        }

        return signatures;
    }
}


// FILE: main.kt

fun box() : String {
    val test = JavaTest()
    val methods = test.reflectConstructors()
    methods.addAll(test.reflectCopies())

    val overloads = listOf(
        Pair("public Data(int)", true),
        Pair("public Data(int,int)", true),
        Pair("public Data(int,long,int,int)", true),
        Pair("public Data(int,long,int,int,int)", false),

        Pair("public final Data Data.copy(int)", true),
        Pair("public final Data Data.copy(int,int)", true),
        Pair("public final Data Data.copy(int,long,int,int)", true),
        Pair("public final Data Data.copy(int,long,int,int,int)", false)
    )

    val overloadCounts = mapOf(
        "Data" to 4,
        "copy" to 4
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
        // NOTE: any constructors with DefaultConstructorMarker are not counted
        val pattern = "$name("
        val actual = methods.count {(sign, _) -> sign.contains(pattern) && !sign.contains("DefaultConstructorMarker") }
        if (actual != expected) return "Fail: overload $name count does not match, expected $expected, actual $actual"
    }

    return "OK"
}
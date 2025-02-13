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
        @Version("1.3") a2: Int = 0,
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

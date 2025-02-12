// FILE: m1.kt

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

//class Simple {
//    @VersionOverloads
//    fun simpleAdd(
//        x: Int,
//        y: Int = 0,
//        @Version("1.2") z1: Int = 0,
//    ) : Int {
//        return x + y + z1
//    }
//}

// FILE: m2.kt

import MyExample.myAdd

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

// FILE: JavaExample.java

public class JavaExample {
    public void f() {
//        MyExample.myAdd(1, 2);
//        MyExample.myAdd(1, 2, 3);
//        MyExample.myAdd(1, 2, 3, 4);
        MyExample.myAdd(1, 2, 3, 4, 5);
        // MyExample.myAdd(1, 2, 3, 4, 5, 6);
    }
}
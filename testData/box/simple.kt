package foo.bar

import com.faizilham.prototype.versioning.*


fun box(): String {
    val result = MyClass().foo()
    return if (result == "Hello world") { "OK" } else { "Fail: $result" }
}

@VersionOverloads
fun myAdd(
    x: Int,
    y: Int = 0,
    @Version("1.2") z1: Int = 0,
    @Version("1.2") z2: Int = 0,
    @Version("1.3") a1: Int = 0,
    @Version("1.3") a2: Int = 0,
) : Int {
    return x + y
}

class Simple {
    @VersionOverloads
    fun simpleAdd(
        x: Int,
        y: Int = 0,
        @Version("1.2") z1: Int = 0,
    ) : Int {
        return x + y
    }
}
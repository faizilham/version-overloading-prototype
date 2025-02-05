// WITH_STDLIB

fun box(): String {
    val list = listOf("aaa", "bb", "c")
    val result = list.map { it.length }.sum()
    return if (result == 6) "OK" else "Fail: $result"
}

@JvmOverloads
fun overloadExample(a: Int, b: Int = 0) = a + b

fun manualOverload(a: Int, b: Int) = a + b
fun manualOverload(a: Int) = manualOverload(a, 0)
import com.faizilham.prototype.versioning.*

const val VERSION_1_1 = "1.1"
const val VERSION_1_2 = "1.2"

fun err1(
  <!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1.1") abc: Int<!>,
) {

}

fun err2(
  a: Int,
  @IntroducedAt("1.1") b: Int = 0,
  c : Int = 0,
  <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>d : Int<!>
) {

}

fun ok1(
  a: Int,
  @IntroducedAt("1") a1: Int = 0,
  b: Int = 0,
  @IntroducedAt(VERSION_1_1) c: Int = 0,
  @IntroducedAt(VERSION_1_2) d: Int = 0,
  callback : () -> Unit
) {

}

fun ok2(
  a: Int,
  @IntroducedAt(VERSION_1_1) a1: Int = 0,
  b: Int = 0,
  @IntroducedAt(VERSION_1_1) c: Int = 0,
  @IntroducedAt(VERSION_1_2) d: Int = 0,
  callback : () -> Unit = { }
) {

}

fun ok3(
  a: Int,
  a1: Int = 0,
  b: Int,
  @IntroducedAt(VERSION_1_1) c: Int = 0,
  callback : () -> Unit
) {

}

fun ok4(
  a: Int,
  a1: Int = 0,
  b: Int,
  c: Int = 0,
  d: Int,
  callback : () -> Unit
) {

}
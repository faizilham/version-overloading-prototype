# Version Overloading Plugin Prototype

This is a Kotlin compiler plugin prototype for version overloading, which adds the annotation `@IntroducedAt(version: String)`. A function or a constructor's optional parameter can be annotated 
with `@IntroducedAt` to indicate in which release version the parameter is added to the function. This plugin generates hidden overload(s)
of said function in order to maintain binary compatibility with older binaries that calls the function. This is particularly useful for maintaining binary
compatibility of your library without needing to manually overload the functions each time there are new optional parameters added.

## Example

Suppose that we released a library exposing a function `makeButton`.

```kt
fun makeButton (
    label: String = "",
    color: Color = DefaultColor,
    onClick: () -> Unit
) { /* body */ }
```

In version 1.1, we update `makeButton` with a new optional parameter `borderColor`. Next, in the latest version 1.2,
we add more optional parameters `borderStyle` and `borderWidth`. We can annotate `makeButton` in the latest version like this:

```kt
fun makeButton (
    label: String = "",
    color: Color  = DefaultColor,
    @IntroducedAt("1.1") borderColor: Color = DefautBorderColor,
    @IntroducedAt("1.2") borderStyle: Style = DefautBorderStyle,
    @IntroducedAt("1.2") borderWidth: Int   = 1,
    onClick: () -> Unit
) { /* body */ }
```

The compiler plugin generates the IR binary of `makeButton` as if it is manually overloaded:

```kt
// the latest version, 1.2
fun makeButton(
    label: String = "",
    color: Color = DefaultColor,
    borderColor: Color = DefautBorderColor,
    borderStyle: Style = DefautBorderStyle,
    borderWidth: Int = 1,
    onClick: () -> Unit
) { /* body */ }

// version 1.1
@Deprecated("Deprecated", level=DeprecationLevel.HIDDEN)
fun makeButton(
    label: String = "",
    color: Color = DefaultColor,
    borderColor: Color = DefautColor,
    onClick: () -> Unit
) = Button(label, color, borderColor, DefautBorderStyle, 1, onClick)

// oldest version, presumably 1.0
@Deprecated("Deprecated", level=DeprecationLevel.HIDDEN)
fun makeButton(
    label: String = "",
    color: Color = DefaultColor,
    onClick: () -> Unit
) = Button(label, color, DefautBorderColor, DefautBorderStyle, 1, onClick)
```

It is also possible to annotate constructor parameters with `@IntroducedAt`.

```kt
data class Button (
    val label: String = "",
    val color: Color = DefaultColor,
    @IntroducedAt("1.1") val borderColor: Color = DefautBorderColor,
    @IntroducedAt("1.2") val borderStyle: Style = DefautBorderStyle,
    @IntroducedAt("1.2") val borderWidth: Int   = 1,
    val onClick: () -> Unit
)
```

In the case of a data class, in addition to the constructor overloads the plugin also generates hidden overloads of the `.copy()` method corresponding to each version of the primary constructor.

## Annotation Validation Rules

The plugin checks the following conditions when validating any instance of `@IntroducedAt` annotation:
1. Version annotations are only added at optional parameters.
2. The version number string conforms to the `java.lang.Runtime.Version` format.
3. Optional parameters with version annotations are in the tail positions or before a trailing lambda,
   and non-optional parameters are in the head. Non-annotated optionals may appear anywhere before trailing lambda.
4. Version annotations are either in increasing order (following the ordering of `java.lang.Runtime.Version`), or must be provided by name when called. 
 This is currently unchecked until the named parameter proposal is implemented, so it is advisable to encourage for providing the optional arguments by names.

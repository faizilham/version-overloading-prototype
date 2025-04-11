package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.psi.KtDeclaration

object Errors {
    val INVALID_NON_OPTIONAL_PARAMETER_POSITION by error0<KtDeclaration>()
    val INVALID_VERSIONING_ON_NON_OPTIONAL by error0<KtDeclaration>()
    val INVALID_VERSION_NUMBER_FORMAT by error0<KtDeclaration>()
    val INVALID_DEFAULT_VALUE_DEPENDENCY by error0<KtDeclaration>()
    val NONFINAL_VERSIONED_FUNCTION by error0<KtDeclaration>()
    val CONFLICT_WITH_JVMOVERLOADS_ANNOTATION by error0<KtDeclaration>()

    val UNORDERED_INTRODUCEDAT_VERSION by warning0<KtDeclaration>()
}
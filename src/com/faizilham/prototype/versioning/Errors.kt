package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.psi.KtDeclaration

object Errors {
    val INVALID_NON_OPTIONAL_PARAMETER_POSITION: KtDiagnosticFactory0 by error0<KtDeclaration>()
    val INVALID_VERSIONING_ON_NON_OPTIONAL: KtDiagnosticFactory0 by error0<KtDeclaration>()
    val INVALID_VERSION_NUMBER_FORMAT: KtDiagnosticFactory0 by error0<KtDeclaration>()
    val INVALID_DEFAULT_VALUE_DEPENDENCY: KtDiagnosticFactory0 by error0<KtDeclaration>()
}
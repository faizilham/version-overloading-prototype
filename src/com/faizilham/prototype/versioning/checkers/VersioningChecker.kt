package com.faizilham.prototype.versioning.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import java.lang.Runtime.Version

object VersioningChecker : FirFunctionChecker(MppCheckerKind.Common) {
    val IntroducedAtAnnotation = FqName("com.faizilham.prototype.versioning.IntroducedAt")
    val IntroducedAtClassId = ClassId.topLevel(IntroducedAtAnnotation)

    val INVALID_NON_OPTIONAL_PARAMETER_POSITION: KtDiagnosticFactory0 by error0<KtDeclaration>()
    val INVALID_VERSIONING_ON_NON_OPTIONAL: KtDiagnosticFactory0 by error0<KtDeclaration>()
    val INVALID_VERSION_NUMBER_FORMAT: KtDiagnosticFactory0 by error0<KtDeclaration>()

    override fun check(func: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        var inVersionedOptionalPart = false

        for ((i, param) in func.valueParameters.withIndex()) {
            val isOptional = param.defaultValue != null
            val versionAnnotation = param.getAnnotationByClassId(IntroducedAtClassId, context.session)

            if (!isOptional) {
                val isTrailingLambda = (i == func.valueParameters.size - 1) &&
                                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                if (inVersionedOptionalPart && !isTrailingLambda) {
                    reporter.reportOn(param.source, INVALID_NON_OPTIONAL_PARAMETER_POSITION, context)
                } else if (versionAnnotation != null) {
                    reporter.reportOn(param.source, INVALID_VERSIONING_ON_NON_OPTIONAL, context)
                }

                continue
            } else if (versionAnnotation == null) continue

            inVersionedOptionalPart = true

            val versionString = versionAnnotation.getStringArgument(Name.identifier("version"), context.session) ?: continue

            try {
                Version.parse(versionString)
            } catch (_: Exception) {
                reporter.reportOn(param.source, INVALID_VERSION_NUMBER_FORMAT, context)
            }
        }
    }
}
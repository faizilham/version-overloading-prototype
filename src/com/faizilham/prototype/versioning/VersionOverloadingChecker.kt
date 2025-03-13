package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.Constants.IntroducedAtClassId
import com.faizilham.prototype.versioning.Errors.INVALID_NON_OPTIONAL_PARAMETER_POSITION
import com.faizilham.prototype.versioning.Errors.INVALID_VERSIONING_ON_NON_OPTIONAL
import com.faizilham.prototype.versioning.Errors.INVALID_VERSION_NUMBER_FORMAT
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.name.Name

object VersionOverloadingChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(func: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        var inVersionedPart = false

        for ((i, param) in func.valueParameters.withIndex()) {
            val isOptional = param.defaultValue != null
            val versionAnnotation = param.getAnnotationByClassId(IntroducedAtClassId, context.session)

            if (!isOptional) {
                val isTrailingLambda = (i == func.valueParameters.size - 1) &&
                                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                if (inVersionedPart && !isTrailingLambda) {
                    reporter.reportOn(param.source, INVALID_NON_OPTIONAL_PARAMETER_POSITION, context)
                } else if (versionAnnotation != null) {
                    reporter.reportOn(param.source, INVALID_VERSIONING_ON_NON_OPTIONAL, context)
                }

                continue
            } else if (versionAnnotation == null) continue

            inVersionedPart = true

            val versionString = versionAnnotation.getStringArgument(Name.identifier("version"), context.session) ?: continue

            try {
                Runtime.Version.parse(versionString)
            } catch (_: Exception) {
                reporter.reportOn(param.source, INVALID_VERSION_NUMBER_FORMAT, context)
            }
        }
    }
}
package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.Constants.IntroducedAtClassId
import com.faizilham.prototype.versioning.Errors.INVALID_NON_OPTIONAL_PARAMETER_POSITION
import com.faizilham.prototype.versioning.Errors.INVALID_VERSIONING_ON_NON_OPTIONAL
import com.faizilham.prototype.versioning.Errors.INVALID_VERSION_NUMBER_FORMAT
import org.jetbrains.kotlin.config.MavenComparableVersion
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

// Checks:
// 1. Version annotations are only added at optional parameters
// 2. The version number conforms to the org.jetbrains.kotlin.config.MavenComparableVersion format
// 3. Optional parameters with version annotations are in the tail positions or before a trailing lambda,
//    and non-optional parameters are in the head. Non-annotated optionals may appear anywhere before trailing lambda.
// 4. [CURRENTLY UNCHECKED] Version annotations are either in increasing order, or must be provided by name

object VersionOverloadingChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private val versionNumberArgument = Name.identifier("version")

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        var inVersionedPart = false

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(IntroducedAtClassId, context.session)

            if (param.defaultValue == null) {
                val isTrailingLambda = (i == declaration.valueParameters.lastIndex) &&
                                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                when {
                    inVersionedPart && !isTrailingLambda ->
                        reporter.reportOn(param.source, INVALID_NON_OPTIONAL_PARAMETER_POSITION, context)
                    versionAnnotation != null ->
                        reporter.reportOn(param.source, INVALID_VERSIONING_ON_NON_OPTIONAL, context)
                }

                continue
            }

            if (versionAnnotation == null) continue

            inVersionedPart = true
            val versionString = versionAnnotation.getStringArgument(versionNumberArgument, context.session) ?: continue

            try {
                MavenComparableVersion(versionString)
            } catch (_: Exception) {
                reporter.reportOn(param.source, INVALID_VERSION_NUMBER_FORMAT, context)
            }
        }
    }
}
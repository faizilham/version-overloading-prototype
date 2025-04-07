package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.Constants.IntroducedAtClassId
import com.faizilham.prototype.versioning.Errors.INVALID_DEFAULT_VALUE_DEPENDENCY
import com.faizilham.prototype.versioning.Errors.INVALID_NON_OPTIONAL_PARAMETER_POSITION
import com.faizilham.prototype.versioning.Errors.INVALID_VERSIONING_ON_NON_OPTIONAL
import com.faizilham.prototype.versioning.Errors.INVALID_VERSION_NUMBER_FORMAT
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name

// Validations:
// 1. Version annotations are only added at optional parameters
// 2. The version number conforms to the org.jetbrains.kotlin.config.MavenComparableVersion format
// 3. Optional parameters with version annotations are in the tail positions or before a trailing lambda,
//    and non-optional parameters are in the head. Non-annotated optionals may appear anywhere before trailing lambda.
// 4. [CURRENTLY UNCHECKED] Version annotations are either in increasing order, or must be provided by name
// 5. If a parameter's default value depends on other parameters, its version must be >= its dependencies versions

object VersionOverloadingChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private val versionNumberArgument = Name.identifier("version")

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        var inVersionedPart = false
        var positionValid = true
        val paramVersion = mutableMapOf<FirCallableSymbol<*>, MavenComparableVersion>()

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(IntroducedAtClassId, context.session)

            if (param.defaultValue == null) {
                val isTrailingLambda = (i == declaration.valueParameters.lastIndex) &&
                                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                when {
                    inVersionedPart && !isTrailingLambda -> {
                        reporter.reportOn(param.source, INVALID_NON_OPTIONAL_PARAMETER_POSITION, context)
                        positionValid = false
                    }
                    versionAnnotation != null -> {
                        reporter.reportOn(param.source, INVALID_VERSIONING_ON_NON_OPTIONAL, context)
                        positionValid = false
                    }
                }

                continue
            }

            if (versionAnnotation == null) continue

            inVersionedPart = true
            val versionString = versionAnnotation.getStringArgument(versionNumberArgument, context.session) ?: continue

            try {
                val version = MavenComparableVersion(versionString)
                paramVersion[param.symbol] = version
            } catch (_: Exception) {
                reporter.reportOn(param.source, INVALID_VERSION_NUMBER_FORMAT, context)
                positionValid = false
            }
        }

        if (positionValid) {
            checkDependency(declaration, context, reporter, paramVersion)
        }
    }

    private fun checkDependency(
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        paramVersions: MutableMap<FirCallableSymbol<*>, MavenComparableVersion>
    ) {
        val visitor = LatestDependencyCollector(paramVersions)

        for (param in declaration.valueParameters) {
            val defaultValue = param.defaultValue ?: continue
            val latestDependency = visitor.getLatestDependency(defaultValue)

            if (!latestDependency.lessThanEqual(paramVersions[param.symbol])){
                reporter.reportOn(param.source, INVALID_DEFAULT_VALUE_DEPENDENCY, context)
            }
        }
    }

    class LatestDependencyCollector(val symbolVersions: Map<FirCallableSymbol<*>, MavenComparableVersion>)
        : FirDefaultVisitor<Unit, LatestDependencyCollector.Context>() {

        class Context(var latestDependency: MavenComparableVersion? = null)

        fun getLatestDependency(expression: FirExpression): MavenComparableVersion? {
            val context = Context()
            expression.accept(this, context)
            return context.latestDependency
        }

        override fun visitElement(element: FirElement, data: Context) {
            element.acceptChildren(this, data)
        }

        override fun visitQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: Context
        ) {
            super.visitQualifiedAccessExpression(qualifiedAccessExpression, data)

            val symbol = qualifiedAccessExpression.toResolvedCallableSymbol() ?: return
            val version = symbolVersions[symbol] ?: return

            if (data.latestDependency.lessThanEqual(version)) {
                data.latestDependency = version
            }
        }
    }

    private fun MavenComparableVersion?.lessThanEqual(other: MavenComparableVersion?): Boolean {
        if (this == null) return true
        if (other == null) return false

        return this <= other
    }
}
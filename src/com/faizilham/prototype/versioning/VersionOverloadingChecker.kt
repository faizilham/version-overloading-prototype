package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.Constants.IntroducedAtClassId
import com.faizilham.prototype.versioning.Constants.JvmOverloadsClassId
import com.faizilham.prototype.versioning.Errors.CONFLICT_WITH_JVMOVERLOADS_ANNOTATION
import com.faizilham.prototype.versioning.Errors.INVALID_DEFAULT_VALUE_DEPENDENCY
import com.faizilham.prototype.versioning.Errors.INVALID_NON_OPTIONAL_PARAMETER_POSITION
import com.faizilham.prototype.versioning.Errors.INVALID_VERSIONING_ON_NON_OPTIONAL
import com.faizilham.prototype.versioning.Errors.INVALID_VERSION_NUMBER_FORMAT
import com.faizilham.prototype.versioning.Errors.NONFINAL_VERSIONED_FUNCTION
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name

// Validations:
// 1. Version annotations are only added at optional parameters
// 2. The version number has the correct format
// 3. Optional parameters with version annotations are in the tail positions or before a trailing lambda,
//    and non-optional parameters are in the head. Non-annotated optionals may appear anywhere before trailing lambda.
// 4. [CURRENTLY UNCHECKED] Version annotations are either in increasing order, or must be provided by name
// 5. If a parameter's default value depends on other parameters, its version must be >= its dependencies versions

object VersionOverloadingChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private val versionNumberArgument = Name.identifier("version")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        var inVersionedPart = false
        var positionValid = true
        var hasVersionAnnotation = false
        val paramVersions = mutableMapOf<FirCallableSymbol<*>, VersionNumber>()

        for ((i, param) in declaration.valueParameters.withIndex()) {
            val versionAnnotation = param.getAnnotationByClassId(IntroducedAtClassId, context.session)

            if (param.defaultValue == null) {
                val isTrailingLambda = (i == declaration.valueParameters.lastIndex) &&
                                        param.returnTypeRef.coneType.isSomeFunctionType(context.session)

                when {
                    inVersionedPart && !isTrailingLambda -> {
                        reporter.reportOn(param.source, INVALID_NON_OPTIONAL_PARAMETER_POSITION)
                        positionValid = false
                    }
                    versionAnnotation != null -> {
                        reporter.reportOn(param.source, INVALID_VERSIONING_ON_NON_OPTIONAL)
                        positionValid = false
                        hasVersionAnnotation = true
                    }
                }

                continue
            }

            if (versionAnnotation == null) continue
            hasVersionAnnotation = true

            inVersionedPart = true
            val versionString = versionAnnotation.getStringArgument(versionNumberArgument, context.session) ?: continue



            try {
                val version = VersionNumber(versionString)
                paramVersions[param.symbol] = version
            } catch (_: Exception) {
                reporter.reportOn(param.source, INVALID_VERSION_NUMBER_FORMAT)
                positionValid = false
            }
        }

        if (hasVersionAnnotation) {
            when {
                declaration.isOverridable() -> {
                    reporter.reportOn(declaration.source, NONFINAL_VERSIONED_FUNCTION)
                    positionValid = false
                }

                declaration.hasAnnotation(JvmOverloadsClassId, context.session) -> {
                    reporter.reportOn(declaration.source, CONFLICT_WITH_JVMOVERLOADS_ANNOTATION)
                    positionValid = false
                }
            }
        }

        if (positionValid) {
            checkDependency(declaration, paramVersions)
        }
    }

    private fun FirFunction.isOverridable(): Boolean = !isFinal || getContainingClass()?.isFinal == false

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDependency(
        declaration: FirFunction,
        paramVersions: Map<FirCallableSymbol<*>, VersionNumber>
    ) {
        val visitor = LatestDependencyCollector(paramVersions)

        for (param in declaration.valueParameters) {
            val defaultValue = param.defaultValue ?: continue
            val latestDependency = visitor.getLatestDependency(defaultValue)

            if (!latestDependency.lessThanEqual(paramVersions[param.symbol])){
                reporter.reportOn(param.source, INVALID_DEFAULT_VALUE_DEPENDENCY)
            }
        }
    }

    class LatestDependencyCollector(val symbolVersions: Map<FirCallableSymbol<*>, VersionNumber>)
        : FirDefaultVisitor<Unit, LatestDependencyCollector.Context>() {

        class Context(var latestDependency: VersionNumber? = null)

        fun getLatestDependency(expression: FirExpression): VersionNumber? {
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

    private fun VersionNumber?.lessThanEqual(other: VersionNumber?): Boolean {
        if (this == null) return true
        if (other == null) return false

        return this <= other
    }
}
package com.faizilham.prototype.versioning.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class VersioningCheckerFirExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers =
        object : DeclarationCheckers() {
            override val functionCheckers = setOf(VersioningChecker)
        }
}
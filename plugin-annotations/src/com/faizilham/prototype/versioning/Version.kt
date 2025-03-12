package com.faizilham.prototype.versioning

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class IntroducedAt(val versionNumber: String)

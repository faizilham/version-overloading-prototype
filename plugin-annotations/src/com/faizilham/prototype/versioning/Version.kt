package com.faizilham.prototype.versioning

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class IntroducedAt(val version: String)

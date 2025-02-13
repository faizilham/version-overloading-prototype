package com.faizilham.prototype.versioning

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Version(val versionNumber: String)


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class VersionOverloads()
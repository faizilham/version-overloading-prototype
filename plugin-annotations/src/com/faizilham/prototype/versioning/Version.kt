package com.faizilham.prototype.versioning

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Version(val number: Int)
package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME

object Constants {
    val IntroducedAtFqName = FqName("com.faizilham.prototype.versioning.IntroducedAt")
    val IntroducedAtClassId = ClassId.Companion.topLevel(IntroducedAtFqName)
    val JvmOverloadsClassId =  ClassId.Companion.topLevel(JVM_OVERLOADS_FQ_NAME)
    val CopyMethodName = Name.identifier("copy")

    val VERSION_OVERLOAD_WRAPPER by IrDeclarationOriginImpl
}


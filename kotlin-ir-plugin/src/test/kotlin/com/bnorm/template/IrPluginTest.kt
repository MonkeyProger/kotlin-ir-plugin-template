/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.template

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.Test

class IrPluginTest {
  @Test
  fun `IR plugin success`() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "main.kt", """
annotation class DebugLog
annotation class From
annotation class To
fun main() {
  println("In de benninging")
  val m = 100
  val p = 1 to 1
println("Vars and body swap test:")
first()
second()
println("Return type swap test:")
println(firstRet())
println(secondRet())
  println(greet(name = "Kotlin IR"))
}
@From
fun first(st: String = "Hello") {
println("1st ${'$'}st, World!")
}
@To
fun second(nd: String = "Not Hello") {
println("2nd ${'$'}nd, World!")
}
@From
fun firstRet(st: String = "String"): String {
return "First fun must be ${'$'}st, not Int!"
}
@To
fun secondRet(nd: Int = 100): Int {
return nd
}
@DebugLog
fun greet(greeting: String = "Hello", name: String = "World"): String {
  return "${'$'}greeting, ${'$'}name!"
}
"""
      )
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    val kClazz = result.classLoader.loadClass("MainKt")
    val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
    main.invoke(null)
  }
}

fun compile(
  sourceFiles: List<SourceFile>,
  plugin: ComponentRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
  return KotlinCompilation().apply {
    sources = sourceFiles
    useIR = true
    compilerPlugins = listOf(plugin)
    inheritClassPath = true
  }.compile()
}

fun compile(
  sourceFile: SourceFile,
  plugin: ComponentRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
  return compile(listOf(sourceFile), plugin)
}

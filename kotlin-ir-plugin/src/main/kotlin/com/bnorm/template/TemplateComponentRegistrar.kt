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

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.name.FqName

val KEY_FUNCTIONS = CompilerConfigurationKey<List<String>>("fully-qualified function names")

@AutoService(ComponentRegistrar::class)
class TemplateComponentRegistrar(
  private val defaultString: String,
  private val defaultFile: String,
  private val functions: Set<FqName>
) : ComponentRegistrar {

  @Suppress("unused") // Used by service loader
  constructor() : this(
    defaultString = "Hello, World!",
    defaultFile = "file.txt",
    functions = emptySet()
  )

  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) {
    val functions = configuration[KEY_FUNCTIONS]?.map { FqName(it) } ?: functions
    //if (functions.isEmpty()) return
    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val string = configuration.get(TemplateCommandLineProcessor.ARG_STRING, defaultString)
    val file = configuration.get(TemplateCommandLineProcessor.ARG_FILE, defaultFile)

    IrGenerationExtension.registerExtension(project, TemplateIrGenerationExtension(messageCollector, functions.toSet(),string))
  }
}



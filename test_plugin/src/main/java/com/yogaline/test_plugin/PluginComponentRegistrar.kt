package com.yogaline.test_plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class PluginComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean
        get() = false

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val projectDir = configuration.getNotNull(PluginCommandLineProcessor.Keys.projectDirKey)
        val logger = FileLogger(projectDir)
        IrGenerationExtension.registerExtension(
            extension = TestExtension(logger),
        )
    }
}

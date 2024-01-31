package com.yogaline.test_plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class PluginCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "com.yogaline.test-compiler-plugin"

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = KEY_PROJECT_DIR,
            valueDescription = "absolute file path",
            description = "directory to save the log file",
            required = true,
            allowMultipleOccurrences = false,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            KEY_PROJECT_DIR -> configuration.put(Keys.projectDirKey, File(value))
        }
    }

    companion object {
        private const val KEY_PROJECT_DIR = "key_project_dir"
    }

    object Keys {
        val projectDirKey = CompilerConfigurationKey<File>(KEY_PROJECT_DIR)
    }
}

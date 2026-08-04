package com.idk.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.idk.apk.patcher.ApkPatcher
import java.io.File

class PatcherCommand : CliktCommand(
    name = "apk-patcher",
    help = "Patch Android APK for custom resolution (360x806@160dpi) and 60fps cap"
) {
    private val inputApk by argument("input", help = "Input APK file")
    private val outputApk by argument("output", help = "Output patched APK file")
    private val sign by option("--sign", help = "Sign the APK after patching").flag()
    private val verbose by option("--verbose", "-v", help = "Verbose output").flag()

    override fun run() {
        try {
            val input = File(inputApk)
            val output = File(outputApk)

            if (!input.exists()) {
                echo("❌ Error: Input APK not found: $inputApk", err = true)
                return
            }

            echo("🔧 APK Patcher v1.0")
            echo("Input:  ${input.absolutePath}")
            echo("Output: ${output.absolutePath}")
            echo("")

            val patcher = ApkPatcher(verbose = verbose)
            patcher.patch(input, output, sign = sign)

            echo("")
            echo("✅ Done!")
            echo("Patched APK: ${output.absolutePath}")
            echo("Size: ${String.format("%.2f MB", output.length() / 1024.0 / 1024.0)}")

        } catch (e: Exception) {
            echo("❌ Error: ${e.message}", err = true)
            if (verbose) {
                e.printStackTrace()
            }
        }
    }
}

fun main(args: Array<String>) = PatcherCommand().main(args)

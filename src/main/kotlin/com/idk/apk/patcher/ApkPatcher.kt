package com.idk.apk.patcher

import org.jf.baksmali.Baksmali
import org.jf.dexlib2.DexFileFactory
import org.jf.smali.Smali
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ApkPatcher(private val verbose: Boolean = false) {

    fun patch(inputApk: File, outputApk: File, sign: Boolean = false) {
        val workDir = File(System.getProperty("java.io.tmpdir"), "apk_work_${System.currentTimeMillis()}")
        
        try {
            workDir.mkdirs()
            log("📦 Extracting APK...")
            extractApk(inputApk, workDir)
            
            log("🔍 Decompiling DEX files...")
            val smaliDirs = decompileDex(workDir)
            
            log("💉 Injecting DisplayMetrics hooks...")
            injectHooks(smaliDirs)
            
            log("🔨 Recompiling DEX files...")
            recompileDex(workDir, smaliDirs)
            
            log("📦 Repackaging APK...")
            repackageApk(workDir, outputApk)
            
            if (sign) {
                log("🔐 Signing APK...")
                signApk(outputApk)
            }
            
        } finally {
            log("🗑️  Cleaning up...")
            workDir.deleteRecursively()
        }
    }

    private fun extractApk(apk: File, workDir: File) {
        ZipFile(apk).use { zip ->
            for (entry in zip.entries()) {
                val file = File(workDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out ->
                        zip.getInputStream(entry).copyTo(out)
                    }
                }
            }
        }
        log("  ✓ Extracted to $workDir")
    }

    private fun decompileDex(workDir: File): List<File> {
        val dexFiles = workDir.walk()
            .filter { it.name.matches(Regex("classes.*\\.dex")) }
            .toList()

        val smaliDirs = mutableListOf<File>()

        for (dexFile in dexFiles) {
            val smaliDir = File(workDir, "smali_${dexFile.nameWithoutExtension}")
            smaliDir.mkdirs()
            smaliDirs.add(smaliDir)

            try {
                val dex = DexFileFactory.loadDexFile(dexFile, null)
                val options = org.jf.baksmali.BaksmaliOptions()
                options.deodex = false
                options.useLocals = true
                options.apiLevel = 34

                Baksmali.disassembleDexFile(dex, smaliDir, null, options)
                log("  ✓ Decompiled ${dexFile.name}")
            } catch (e: Exception) {
                log("  ⚠️  Error decompiling ${dexFile.name}: ${e.message}")
            }
        }

        return smaliDirs
    }

    private fun injectHooks(smaliDirs: List<File>) {
        for (smaliDir in smaliDirs) {
            log("  → Patching ${smaliDir.name}...")
            patchSmaliDirectory(smaliDir)
        }
    }

    private fun patchSmaliDirectory(smaliDir: File) {
        var filesPatched = 0

        smaliDir.walk()
            .filter { it.isFile && it.name.endsWith(".smali") }
            .forEach { smaliFile ->
                if (patchSmaliFile(smaliFile)) {
                    filesPatched++
                }
            }

        log("    ✓ Patched $filesPatched files")
    }

    private fun patchSmaliFile(smaliFile: File): Boolean {
        var content = smaliFile.readText()
        val originalContent = content

        content = content.replace(
            Regex("""new-instance\s+([vp]\d+),\s*Landroid/util/DisplayMetrics;"""),
            """invoke-static {}, Lcom/idk/gamecloner/MetricsHook;->createSpoofedMetrics()Landroid/util/DisplayMetrics;
    move-result-object ${'$'}1"""
        )

        if (content.contains("Landroid/app/Activity;")) {
            val hookMethod = """

    # [Injected] DisplayMetrics spoofing hook
    .method public getResources()Landroid/content/res/Resources;
        .registers 2
        invoke-super {p0}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;
        move-result-object v0
        invoke-static {v0}, Lcom/idk/gamecloner/MetricsHook;->applySpoofedMetrics(Landroid/content/res/Resources;)V
        return-object v0
    .end method"""

            if (!content.contains("applySpoofedMetrics")) {
                content = content.replace(".end class", "$hookMethod\n.end class")
            }
        }

        if (content != originalContent) {
            smaliFile.writeText(content)
            return true
        }

        return false
    }

    private fun recompileDex(workDir: File, smaliDirs: List<File>) {
        for (smaliDir in smaliDirs) {
            val dexName = smaliDir.name.replace("smali_", "")
            val outputDex = File(workDir, "$dexName.dex")

            try {
                val options = org.jf.smali.SmaliOptions()
                Smali.assembleSmaliFiles(setOf(smaliDir), outputDex, null, options)
                log("  ✓ Recompiled $dexName.dex")
            } catch (e: Exception) {
                log("  ⚠️  Error recompiling $dexName: ${e.message}")
            }
        }
    }

    private fun repackageApk(workDir: File, outputApk: File) {
        ZipOutputStream(outputApk.outputStream()).use { zip ->
            workDir.walk().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(workDir).path.replace(File.separator, "/")
                    
                    if (!relativePath.contains("smali")) {
                        val entry = ZipEntry(relativePath)
                        zip.putNextEntry(entry)
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
        log("  ✓ Repackaged APK")
    }

    private fun signApk(apk: File) {
        val debugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")

        if (!debugKeystore.exists()) {
            log("  ⚠️  Debug keystore not found - skipping signing")
            return
        }

        try {
            val command = listOf(
                "jarsigner",
                "-verbose",
                "-sigalg", "SHA1withRSA",
                "-digestalg", "SHA1",
                "-keystore", debugKeystore.absolutePath,
                "-storepass", "android",
                "-keypass", "android",
                apk.absolutePath,
                "androiddebugkey"
            )

            val process = ProcessBuilder(command).start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                log("  ✓ Signed with debug key")
            }
        } catch (e: Exception) {
            log("  ⚠️  Signing error: ${e.message}")
        }
    }

    private fun log(message: String) {
        if (verbose || !message.contains("→")) {
            println(message)
        }
    }
}

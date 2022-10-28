package com.shocker.hideapk.hide

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.shocker.hideapk.BuildConfig.APPLICATION_ID
import com.shocker.hideapk.signing.JarMap
import com.shocker.hideapk.signing.SignApk
import com.shocker.hideapk.utils.AXML
import com.shocker.hideapk.utils.Keygen
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.UiThreadHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.SecureRandom

object HideAPK {

    private const val TAG = "HIDE"
    private const val ALPHA = "abcdefghijklmnopqrstuvwxyz"
    private const val ALPHADOTS = "$ALPHA....."
    private const val ANDROID_MANIFEST = "AndroidManifest.xml"

    // Generate random package names
    private fun genPackageName(): String {
        val random = SecureRandom()
        val len = 5 + random.nextInt(15)
        val builder = StringBuilder(len)
        var next: Char
        var prev = 0.toChar()
        for (i in 0 until len) {
            next = if (prev == '.' || i == 0 || i == len - 1) {
                ALPHA[random.nextInt(ALPHA.length)]
            } else {
                ALPHADOTS[random.nextInt(ALPHADOTS.length)]
            }
            builder.append(next)
            prev = next
        }
        if (!builder.contains('.')) {
            // Pick a random index and set it as dot
            val idx = random.nextInt(len - 2)
            builder[idx + 1] = '.'
        }
        return builder.toString()
    }

    fun patch(
        context: Context,
        sourceApkFile: File,
        patchApk: OutputStream,
        pkg: String,
        label: CharSequence
    ): Boolean {
        // 根据apk文件获取Apk文件获取 packageInfo
        val sourceAppInfo =
            context.packageManager.getPackageArchiveInfo(sourceApkFile.path, 0) ?: return false

        val sourceAppLabel = sourceAppInfo.applicationInfo.nonLocalizedLabel.toString()
        try {
            JarMap.open(sourceApkFile, true).use { jar ->
                val je = jar.getJarEntry(ANDROID_MANIFEST)
                val xml = AXML(jar.getRawData(je)) //打开原apk的AndroidManifest.xml文件

                // path Manifest.xml file
                if (!xml.findAndPatch(APPLICATION_ID to pkg, sourceAppLabel to label.toString()))
                    return false

                // Write apk changes
                jar.getOutputStream(je).use { it.write(xml.bytes) }
                val keys = Keygen(context)
                // sign patched apk file
                SignApk.sign(keys.cert, keys.key, jar, patchApk)
                return true
            }
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    private fun patchAndHide(
        activity: Activity,
        label: String,
        onFailure: Runnable,
        path: String
    ): Boolean {
        // Get its own apk file "/data/app/~~ElZ8Krroj9NnI_4637t52g==/com.shocker.hideapk-EQFbaRvebkdstrVAQBck8g==/base.apk"
        val stub = File(path)

        // Generate a new random package name and signature  "/data/user/0/com.shocker.hideapk/cache/patched.apk"
        val repack = File(activity.cacheDir, "patched.apk")

        val randomPkg = genPackageName()

        Log.d(TAG, "random package name :$randomPkg")

        if (!patch(activity, stub, FileOutputStream(repack), randomPkg, label))
            return false

        val cmd = "pm install ${repack.absolutePath}"
        return if (Shell.su(cmd).exec().isSuccess) {
            UiThreadHandler.run {
                Toast.makeText(
                    activity,
                    "随机包名安装成功,应用名:${label},",
                    Toast.LENGTH_LONG
                ).show()
            }
            true
        } else {
            UiThreadHandler.run { Toast.makeText(activity, "随机包名安装失败", Toast.LENGTH_LONG).show() }
            false
        }

    }

    //label:应用的名称
    //path:原apk安装包路径
    suspend fun hide(activity: Activity, label: String, path: String) {
        val onFailure = Runnable {

        }
        val success = withContext(Dispatchers.IO) {
            patchAndHide(activity, label, onFailure, path)
        }
        if (!success) onFailure.run()
    }

}

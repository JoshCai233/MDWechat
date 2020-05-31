package com.blanke.mdwechat.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.Toast
import com.blanke.mdwechat.Common
import com.blanke.mdwechat.config.AppCustomConfig
import com.blanke.mdwechat.settings.view.GetNewestVersion
import com.blanke.mdwechat.util.FileUtils
import com.blanke.mdwechat.util.LogUtil
import com.joshcai.mdwechat.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread


/**
 * Created by blanke on 2017/6/8.
 */

class SettingsActivity : Activity() {
//    var a: Activity? = null
//    fun get(): Activity? {
//        if (a == null) {
//            a = SettingsActivity()
//        }
//        return a
//    }

    private lateinit var fab: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        Common.APP_DIR_PATH
        verifyStoragePermissions(this)
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            copyConfig()
            _clearLogs()
            goToWechatSettingPage()
        }
        GetNewestVersion.show(this, getVersionCode())
    }

    private fun _clearLogs() {
        LogUtil.clearFileLogs(SettingsFragment.STATIC.isLogFile)
        Toast.makeText(this, getString(R.string.msg_clear_ok), Toast.LENGTH_SHORT).show()
    }

    private fun showSettingsFragment() {
        findViewById<View>(R.id.pb_loading).visibility = View.GONE
        fab.visibility = View.VISIBLE
        fragmentManager.beginTransaction().replace(R.id.setting_fl_container,
                SettingsFragment()).commit()
    }

    private fun copySharedPrefences() {
        val sharedPrefsDir = File(filesDir, "../shared_prefs")
        val sharedPrefsFile = File(sharedPrefsDir, Common.MOD_PREFS + ".xml")
        val sdSPFile = File(AppCustomConfig.getConfigFile(Common.MOD_PREFS + ".xml"))
        SettingsFragment.STATIC.sharedPrefsFile = sharedPrefsFile
        SettingsFragment.STATIC.sdSPFile = sdSPFile
        if (sharedPrefsFile.exists()) {
            val outStream = FileOutputStream(sdSPFile)
            FileUtils.copyFile(FileInputStream(sharedPrefsFile), outStream)
        } else if (sdSPFile.exists()) { // restore sharedPrefsFile
            sharedPrefsFile.parentFile.mkdirs()
            val input = FileInputStream(sdSPFile)
            val outStream = FileOutputStream(sharedPrefsFile)
            FileUtils.copyFile(input, outStream)
        }
    }

    private fun goToWechatSettingPage() {
        Toast.makeText(this, R.string.msg_kill_wechat, Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", Common.WECHAT_PACKAGENAME, null)
        startActivity(intent)
    }

    private fun copyConfig() {
        thread {
            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.CONFIG_WECHAT_DIR)
            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.CONFIG_VIEW_DIR)
//            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.CONFIG_readme_DIR)
            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.ICON_DIR)
            copySharedPrefences()
            Handler(Looper.getMainLooper()).post {
                showSettingsFragment()
            }
        }
    }

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE")

    fun verifyStoragePermissions(activity: Activity) {
        try {
            val permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
//                    && ActivityCompat.checkSelfPermission(activity,
//                    "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED
            if (!permission) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
            } else {
                copyConfig()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                copyConfig()
            } else {
                Toast.makeText(this, R.string.msg_permission_fail, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun getVersionCode(): Int {
        // 包管理器 可以获取清单文件信息
        try {
            // 获取包信息
            // 参1 包名 参2 获取额外信息的flag 不需要的话 写0
            val packageInfo: PackageInfo = packageManager.getPackageInfo(
                    packageName, 0)
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return 0
    }
}
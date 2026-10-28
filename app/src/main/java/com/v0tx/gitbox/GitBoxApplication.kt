package com.v0tx.gitbox

import android.app.Application

class GitBoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // JGit 会读取 user.home 作为全局 git 配置的根目录，
        // Android 上默认不可写，这里指向应用私有目录。
        runCatching {
            System.setProperty("user.home", filesDir.absolutePath)
        }
    }
}

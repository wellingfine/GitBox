package com.v0tx.gitbox.data.git

import com.v0tx.gitbox.data.model.StorageConfig
import com.v0tx.gitbox.data.model.SyncResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.EmptyCommitException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * JGit 封装：负责 clone / commit / push / pull。
 * 仅支持 http(s) 协议 + token 认证（私有仓库填写 token，公有仓库可留空）。
 */
class GitEngine(private val dir: File, private val cfg: StorageConfig) {

    private fun credentials(): CredentialsProvider? =
        if (cfg.token.isBlank()) null
        else UsernamePasswordCredentialsProvider(cfg.userName.ifBlank { "git" }, cfg.token)

    fun isInitialized(): Boolean = File(dir, ".git").exists()

    /** 打开已有仓库，没有就先克隆或初始化 */
    fun open(): Git {
        if (isInitialized()) {
            val git = Git.open(dir)
            ensureRemote(git)
            return git
        }
        dir.mkdirs()
        val git = runCatching {
            Git.cloneRepository()
                .setURI(cfg.gitUrl)
                .setDirectory(dir)
                .setCredentialsProvider(credentials())
                .call()
        }.getOrElse {
            // 远端是空仓库或暂不可用时，退化成本地初始化，之后首次提交再推送
            Git.init().setDirectory(dir).setInitialBranch(cfg.branch).call()
        }
        ensureRemote(git)
        return git
    }

    private fun ensureRemote(git: Git) {
        if (cfg.gitUrl.isBlank()) return
        runCatching {
            val config = git.repository.config
            if (config.getString("remote", "origin", "url") != cfg.gitUrl) {
                config.setString("remote", "origin", "url", cfg.gitUrl)
                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
                config.save()
            }
        }
    }

    /** 从远端拉取最新内容 */
    fun pull(): SyncResult {
        if (cfg.gitUrl.isBlank()) return SyncResult(true, "本地仓库无需拉取")
        return try {
            val git = open()
            val repo = git.repository
            val branch = repo.branch ?: cfg.branch
            if (repo.resolve("HEAD") == null) {
                git.fetch().setRemote("origin").setCredentialsProvider(credentials()).call()
                val candidates = listOf(branch, "main", "master").distinct()
                val remote = candidates.firstOrNull { repo.resolve("refs/remotes/origin/$it") != null }
                if (remote != null) {
                    git.checkout()
                        .setCreateBranch(true)
                        .setName(remote)
                        .setStartPoint("origin/$remote")
                        .call()
                }
                SyncResult(true, "已获取远端内容")
            } else {
                val result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branch)
                    .setCredentialsProvider(credentials())
                    .call()
                if (result.isSuccessful) {
                    SyncResult(true, "已同步")
                } else {
                    SyncResult(false, "同步失败：${result.mergeResult?.mergeStatus ?: "未知原因"}")
                }
            }
        } catch (e: Exception) {
            SyncResult(false, readable(e))
        }
    }

    /** 提交全部变更并推送 */
    fun commitAndPush(message: String): SyncResult {
        val git = try {
            open()
        } catch (e: Exception) {
            return SyncResult(false, readable(e))
        }
        return try {
            val repo = git.repository
            val branch = repo.branch ?: cfg.branch
            git.add().addFilepattern(".").setUpdate(true).call()
            git.add().addFilepattern(".").call()
            val status = git.status().call()
            if (!status.isClean) {
                git.commit()
                    .setAuthor(cfg.authorName, cfg.authorEmail)
                    .setMessage(message)
                    .call()
            }
            if (cfg.gitUrl.isBlank()) {
                SyncResult(true, "已提交到本地仓库")
            } else {
                pushInternal(git, branch)
            }
        } catch (e: EmptyCommitException) {
            SyncResult(true, "没有需要提交的变更")
        } catch (e: Exception) {
            SyncResult(false, readable(e))
        }
    }

    /** 把本地已有的提交推送到远端 */
    fun pushPending(): SyncResult {
        if (cfg.gitUrl.isBlank()) return SyncResult(true, "本地仓库无需推送")
        return try {
            val git = open()
            pushInternal(git, git.repository.branch ?: cfg.branch)
        } catch (e: Exception) {
            SyncResult(false, readable(e))
        }
    }

    private fun pushInternal(git: Git, branch: String): SyncResult {
        val results = git.push()
            .setRemote("origin")
            .setCredentialsProvider(credentials())
            .add(branch)
            .call()
        val failures = mutableListOf<String>()
        results.forEach { result ->
            result.remoteUpdates.forEach { update ->
                when (update.status) {
                    RemoteRefUpdate.Status.OK, RemoteRefUpdate.Status.UP_TO_DATE -> Unit
                    else -> failures.add("${update.remoteName}: ${update.status}")
                }
            }
        }
        return if (failures.isEmpty()) {
            SyncResult(true, "已推送到远端")
        } else {
            SyncResult(false, "推送失败：${failures.joinToString("; ")}")
        }
    }

    /** 删除本地仓库副本（配置页“重置本地仓库”使用） */
    fun resetLocal(): Boolean = runCatching { dir.deleteRecursively() }.isSuccess

    private fun readable(e: Exception): String =
        e.message?.take(300) ?: e.javaClass.simpleName
}

package lxtx.im.util.filetransfer.download

import androidx.lifecycle.LifecycleOwner
import eth.ELive
import eth.ext.getDownloadName
import lib.chat.model.Time
import lib.chat.net.DownloadResult
import lib.chat.util.Caching
import lxtx.im.db.Db
import lxtx.im.design.repo.ChatRepo
import lxtx.im.design.viewModel.chat.chatPlugin.NetToast
import lxtx.im.model.download.DownloadInfo
import vector.EMPTY
import vector.ext.toast
import java.io.File

/**
 * @author 宁锟
 * @since 2020/3/27
 */
class DefaultDownloader(private val url: String) : FileDownloader() {

    companion object {
        private const val PROGRESS_REFRESH_INTERVAL = 500L
        private const val PROGRESS_INTERVAL = 5f
    }

    val chatRepo = ChatRepo()
    private var liveDownload: ELive<DownloadResult>? = null
    private var lastProgress = -PROGRESS_INTERVAL
    private var lastTime = 0L
    private var contentLengthIsKnown = false//是否已经知道文件的总长度
    private var baseProgress = 0f//初始进度

    override fun execute(owner: LifecycleOwner?) {
        beforeExecute()
        liveDownload = chatRepo.download(url, getRange(url))
                .progress { progress, contentLength ->
                    updateDownloadInfoInfNeed(url, contentLength)
                    val realProgress = baseProgress + progress

                    if ((needUpdateProgress(realProgress, lastProgress))) {
                        this.progress?.invoke(realProgress / 100f, contentLength)
                        lastProgress = realProgress
                        lastTime = Time.serviceTime
                    }

                }.observe { result ->
                    this.downloadResult?.invoke(result.path.plus(File.separator).plus(result.name))
                    DownloadFactory.onDownloadComplete(this)
                }.error {
                    toast(NetToast.getString(it))
                    this.error?.invoke(it)
                    DownloadFactory.onDownloadComplete(this)
                }
        liveDownload?.load(owner)
    }

    override fun cancel() {
        liveDownload?.dispose()

    }

    /**
     * 在上传前作准备工作:一些变量的初始化
     */
    private fun beforeExecute() {
        val downloadInfo = Db.sync {
            queryDownloadInfo(url.getDownloadName())
        }
        if (downloadInfo != null) {
            contentLengthIsKnown = true
            val downTempFile = getDownloadTempFile(url)
            if (downTempFile.exists()) {
                baseProgress = downTempFile.length().toFloat() / downloadInfo.contentLength.toFloat() * 100
            }
        }
    }

    /**
     * 更新文件总长度
     */
    private fun updateDownloadInfoInfNeed(url: String, contentLength: Long) {
        if (contentLengthIsKnown) return
        Db.async {
            insert(DownloadInfo().apply {
                downloadName = url.getDownloadName()
                this.contentLength = contentLength
            })
        }
        contentLengthIsKnown = true
    }

    private fun getRange(url: String): String {
        val downTempFile = getDownloadTempFile(url)
        return if (downTempFile.exists()) {//是否需要断点续传
            //查询数据库，获取下载的文件大小
            var contentLength = Db.sync {
                queryDownloadInfo(url.getDownloadName())?.contentLength ?: 0L
            }
            if (contentLength == 0L) {
                EMPTY
            } else {
                "bytes=".plus(downTempFile.length()).plus("-").plus(contentLength - 1)
            }
        } else {
            EMPTY
        }
    }

    private fun getDownloadTempFile(url: String): File {
        return File(Caching.downloadCachePath, url.getDownloadName().plus(".temp"))
    }

    private fun needUpdateProgress(progress: Float, lastProgress: Float): Boolean {
        //进度会出现倒退问题,如果当前进度小于上一个进度，就不进行回调
        return (progress >= lastProgress && progress - lastProgress >= PROGRESS_INTERVAL && Time.serviceTime - lastTime >= PROGRESS_REFRESH_INTERVAL)
                || progress == 100f
    }

}
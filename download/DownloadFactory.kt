package lxtx.im.util.filetransfer.download

import lib.chat.Constants
import lxtx.im.model.ChatMessage
import lxtx.im.model.filetranfer.Transferable
import lxtx.im.util.filetransfer.download.multipart.MultipartDownloader

/**
 * 根据输入参数不同，选择不同的下载策略
 * @author 宁锟
 * @since 2020/3/27
 */
object DownloadFactory {
    private val downLoadingMap = hashMapOf<String, FileDownloader>()//记录正在下载的FileDownloader key是消息id

    //等待被取消的消息mid集合 应用场景：用户或测试人员快速点击文件下载和暂停的时候，由于下载是要经过service做处理所以会有一定的延时，
    // 所以实际的处理顺序变成暂停下载，开始下载
    private val pendingCancel = arrayListOf<String>()
    private val emptyDownloader = EmptyDownloader()
    fun get(url: String, message: ChatMessage? = null): FileDownloader {
        //是否已经取消了
        if (isCanceled(message?.mid)) {
            pendingCancel.remove(message?.mid)
            return emptyDownloader
        }
        //已经在下载中了，不需要再下载
        if (downLoading(message?.mid)) {
            return emptyDownloader
        }

        val downloader = getSuitableDownloader(url, message)
        message?.let { downLoadingMap.put(it.mid, downloader) }
        return downloader
    }

    /**
     * 获取合适的下载器
     */
    private fun getSuitableDownloader(url: String, message: ChatMessage? = null): FileDownloader {
        return if (message is Transferable && isSupportMultiPartDownload(message as Transferable)) {
            MultipartDownloader(message as Transferable)
        } else {
            DefaultDownloader(adjustUrl(url))
        }
    }

    /**
     * 调整下载链接
     */
    private fun adjustUrl(url: String): String {
        return Constants.getDownloadHead(url).plus(url)
    }

    /**
     * 当前消息是否已经取消了下载
     */
    private fun isCanceled(mid: String?): Boolean {
        return pendingCancel.contains(mid)
    }

    /**
     * mid对应的消息是否处于下载状态
     */
    fun downLoading(mid: String?): Boolean {
        return downLoadingMap.containsKey(mid)
    }

    /**
     * 下载完成或发生错误，移除对应的FileDownloader
     */
    fun onDownloadComplete(downloader: FileDownloader) {
        for (entry in downLoadingMap.entries) {
            if (entry.value == downloader) {
                downLoadingMap.remove(entry.key)
                break
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancel(mid: String) {
        if (downLoadingMap.containsKey(mid)) {
            downLoadingMap[mid]?.cancel()
            downLoadingMap.remove(mid)
        } else {
            pendingCancel.add(mid)
        }
    }

    /**
     * 是否支持多任务下载
     */
    private fun isSupportMultiPartDownload(transferable: Transferable) = transferable.obtainFileId().isNotEmpty()
}
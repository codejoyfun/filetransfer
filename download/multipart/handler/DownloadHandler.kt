package lxtx.im.util.filetransfer.download.multipart.handler

import androidx.lifecycle.LifecycleOwner
import eth.ELive
import lib.chat.net.DownloadResult
import lxtx.im.util.filetransfer.FileBaseHandler
import lxtx.im.util.filetransfer.download.multipart.DownloadRequest
import lxtx.im.util.filetransfer.upload.multipart.FileRepo
import java.io.File

/**
 * @author ningkun
 * @date   2019/12/4
 */
class DownloadHandler : FileBaseHandler<DownloadRequest>() {

    private val repo = FileRepo()
    private var progress: ((Float, Int) -> Unit)? = null
    private var result: ((String) -> Unit)? = null
    private var owner: LifecycleOwner? = null
    private var downloadLive: ELive<DownloadResult>? = null

    fun onProgress(progress: (progress: Float, index: Int) -> Unit) = this.apply { this.progress = progress }

    fun onComplete(result: (downloadPath: String) -> Unit) = this.apply { this.result = result }

    fun bindLifecycle(owner: LifecycleOwner? = null) = this.apply { this.owner = owner }

    fun cancel() {
        downloadLive?.dispose()
    }

    override fun process(request: DownloadRequest) {
        downloadLive = repo.download(request.fileId, request.chunkIndex).observe { downloadResult ->
            val downloadPath = downloadResult.path.plus(File.separator).plus(downloadResult.name)
            this.result?.invoke(downloadPath)
            nextHandler?.process(request.newBuilder()
                    .downloadPath(downloadPath)
                    .suffix(downloadResult.suffix.orEmpty())
                    .build())
        }.progress { progress, _ ->
            //分片下载的接口无法直接获取下载进度和文件长度的中间值，用获取本地文件长度做替代
            this.progress?.invoke(progress, request.chunkIndex)
        }
        downloadLive?.load(owner)
    }
}
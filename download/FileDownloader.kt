package lxtx.im.util.filetransfer.download

import androidx.lifecycle.LifecycleOwner
import live.Error

/**
 * @author 宁锟
 * @since 2020/3/24
 */
abstract class FileDownloader {
    protected var progress: ((Float, Long) -> Unit)? = null//进度回调
    protected var downloadResult: ((downloadResult: String) -> Unit)? = null//上传成功回调
    protected var error: ((err: Error) -> Unit)? = null//错误回调
    protected var owner: LifecycleOwner? = null//生命周期

    fun onProgress(progress: (progress: Float, contentLength: Long) -> Unit) = this.apply { this.progress = progress }

    fun onError(error: (err: Error) -> Unit) = this.apply { this.error = error }

    fun onComplete(downloadResult: (downloadResult: String) -> Unit) = this.apply { this.downloadResult = downloadResult }

    abstract fun execute(owner: LifecycleOwner? = null)//执行传输
    abstract fun cancel()//取消传输
}
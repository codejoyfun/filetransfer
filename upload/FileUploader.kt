package lxtx.im.util.filetransfer.upload

import androidx.lifecycle.LifecycleOwner
import lxtx.im.model.CommonUploadResult

/**
 * 文件上传基类
 * @author ningkun
 * @date   2020/1/6
 */
abstract class FileUploader {
    protected var progress: ((Float, Long) -> Unit)? = null//进度回调
    protected var uploadResult: ((uploadResult: CommonUploadResult) -> Unit)? = null//上传成功回调
    protected var error: (() -> Unit)? = null//错误回调
    protected var cancel: (() -> Boolean)? = null//是否取消
    protected var owner: LifecycleOwner? = null//生命周期

    fun onProgress(progress: (progress: Float, contentLength: Long) -> Unit) = this.apply { this.progress = progress }

    fun onError(error: () -> Unit) = this.apply { this.error = error }

    fun onComplete(uploadResult: (uploadResult: CommonUploadResult) -> Unit) = this.apply { this.uploadResult = uploadResult }

    abstract fun execute(owner: LifecycleOwner? = null)//执行传输
    abstract fun cancel()//取消传输
}

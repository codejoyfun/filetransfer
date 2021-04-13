package lxtx.im.util.filetransfer.upload

import androidx.lifecycle.LifecycleOwner
import eth.ELive
import eth.ext.onProgress
import eth.toLive
import lib.chat.model.UploadType
import lib.chat.net.NET
import live.Live
import lxtx.im.CommonApi
import lxtx.im.model.CommonUploadResult
import vector.EMPTY

/**
 * 默认的上传方式
 * @author 宁锟
 * @since 2020/3/24
 */
class DefaultUploader(private val localFilePath: String) : FileUploader() {

    private val liveUpload = Live<CommonUploadResult>()
    private var request = ELive<CommonUploadResult>()

    override fun execute(owner: LifecycleOwner?) {
        request = NET.create(CommonApi::class)
                .upload(localFilePath, UploadType.CHAT).onProgress { progress, contentLength ->
                    this@DefaultUploader.progress?.invoke(progress, contentLength)
                }.map {
                    CommonUploadResult(EMPTY, EMPTY, it.path.orEmpty(), it.host.orEmpty())
                }.toLive()
        liveUpload.execute(request).observe {
            uploadResult?.invoke(it)
        }
    }

    override fun cancel() {
        request.dispose()
    }
}
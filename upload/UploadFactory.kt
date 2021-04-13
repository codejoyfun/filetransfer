package lxtx.im.util.filetransfer.upload

import lib.chat.model.UploadType
import lxtx.im.model.ChatMessage
import lxtx.im.model.filetranfer.Transferable
import lxtx.im.util.filetransfer.upload.multipart.MultipartUploader

/**
 * 上传工厂类,根据不同的场景返回具体的上传类
 * @author 宁锟
 * @since 2020/3/25
 */
object UploadFactory {
    fun get(localFilePath: String, message: ChatMessage? = null, uploadType: String = UploadType.CHAT): FileUploader {
        // 用户用旧版的分片上传，只上传了部分分片，更新版本后，如果发现是这种情况，就继续用分片上传的方式
        return if (message is Transferable && message.obtainFileId().isNotEmpty()) {
            MultipartUploader(message as Transferable)
        } else {//除了上述情况，一律采用七牛云上传
            QiNiuYunUploader(localFilePath, uploadType)
        }
    }

    ///////////////////////////储存未完成的上传进度/////////////////////////////////////////////////
    //mid为key 上传进度为value
    private val cacheProgressMap = HashMap<String, Float>()

    fun getCacheProgress(mid: String): Float {
        return cacheProgressMap[mid] ?: -1f
    }

    fun setCacheProgress(mid: String, progress: Float) {
        cacheProgressMap[mid] = progress
    }

}
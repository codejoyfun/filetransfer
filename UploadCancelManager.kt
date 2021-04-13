package lxtx.im.util.filetransfer

import lxtx.im.MessageStatus
import lxtx.im.R
import lxtx.im.db.Db
import lxtx.im.util.filetransfer.upload.FileUploader
import vector.ext.toast

/**
 * 取消上传的管理类，通过map存储mid和FileUploader的键值对
 * 当用户在聊天记录界面进行取消上传操作时，拿到mid，找到对应的FileUploader，调用其cancel方法
 * @author 宁锟
 * @since 2020/6/5
 */
object UploadCancelManager {
    private val fileUploaderMap = hashMapOf<String, FileUploader>()
    private val cancelMids = arrayListOf<String>()//记录已经取消上传的mid

    fun register(mid: String, fileUploader: FileUploader) {
        fileUploaderMap[mid] = fileUploader
    }

    /**
     * 返回取消的操作结果
     */
    fun cancel(mid: String): Boolean {
        return if (canCancel(mid)) {
            fileUploaderMap[mid]?.cancel()
            cancelMids.add(mid)
            true
        } else {
            toast(R.string.chat_already_uploaded)
            false
        }
    }

    private fun canCancel(mid: String): Boolean {
        val message = Db.sync {
            queryMessageByMid(mid)
        }
        return message?.status != MessageStatus.SENDING
    }

    fun isAlreadyCancel(mid: String): Boolean {
        return cancelMids.contains(mid)
    }

}
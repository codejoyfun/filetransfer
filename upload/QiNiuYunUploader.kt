package lxtx.im.util.filetransfer.upload

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import lib.chat.model.Time
import lib.chat.model.UploadType
import lib.chat.util.Caching
import lib.qiniuyun.upload.UploadRequest
import lib.qiniuyun.upload.UploadUtil
import lxtx.im.model.CommonUploadResult
import lxtx.im.util.ChatFileUtil
import lxtx.im.util.UUIDEx
import lxtx.im.util.filetransfer.QiNiuYunUtil
import vector.EMPTY
import vector.appContext
import vector.compat.file.FileCompat
import vector.ext.isUri
import vector.util.DeviceUtil
import vector.util.TimeFormatter
import java.io.File
import java.util.*

/**
 * 七牛云上传
 * @author 宁锟
 * @since 2020/3/24
 */
class QiNiuYunUploader(private var localFilePath: String, private var type: String = UploadType.CHAT) : FileUploader() {


    @Volatile
    private var isCanceled = false

    companion object {
        private const val DIR_QI_NIU_YUN = "qiniuyun"
        private const val PROGRESS_REFRESH_INTERVAL = 500L
        private const val PROGRESS_INTERVAL = 0.05f
    }

    override fun execute(owner: LifecycleOwner?) {
        QiNiuYunUtil.getConfig(owner, {
            upload(it.upToken, it.region, it.downloadHost, owner)
        }, {
            error?.invoke()
        })
    }

    private fun upload(token: String, region: String, downloadHost: String, owner: LifecycleOwner?) {
        var lastTime = 0L
        var lastProgress = -PROGRESS_INTERVAL

        if (DeviceUtil.isOverAndroidQ && localFilePath.isUri()) {
            val fileInfo = ChatFileUtil.getFileInfo(Uri.parse(localFilePath))
            localFilePath = fileInfo.filePath
        }
        val fileSize = FileCompat.getSize(localFilePath, appContext)
        val keyName = genKeyName(UploadType.map[type].orEmpty(), UUIDEx.random().plus(".").plus(ChatFileUtil.getSuffix(localFilePath)))
        val request = UploadRequest().newBuilder().token(token).region(region).filePath(localFilePath).keyName(keyName).progress {
            //控制回调频率
            if ((Time.serviceTime - lastTime >= PROGRESS_REFRESH_INTERVAL && it - lastProgress >= PROGRESS_INTERVAL) || it == 1f) {
                progress?.invoke(it, fileSize)
                lastTime = Time.serviceTime
                lastProgress = it
            }
        }.uploadResult { path ->
            uploadResult?.invoke(CommonUploadResult(EMPTY, EMPTY, path,
                    if (downloadHost.last() == '/') downloadHost else downloadHost.plus("/")))
        }.error {
            error?.invoke()
        }.cancel {
            isCanceled || owner?.lifecycle?.currentState == Lifecycle.State.DESTROYED
        }.build()
        progress?.invoke(0f, fileSize)//立马手动回调一次进度，以便在UI界面上能展示进度
        UploadUtil.upload(request, Caching.uploadCacheDir.plus(File.separator).plus(DIR_QI_NIU_YUN))
    }

    override fun cancel() {
        isCanceled = true
    }

    private fun genKeyName(type: String, fileName: String): String {
        return java.lang.StringBuilder().append("qiniu/").append("fincy/").append(type).append("/").append(TimeFormatter.ymd(Date())).append("/").append(fileName).toString()
    }

}
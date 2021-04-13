package lxtx.im.util.filetransfer.upload.multipart.handler

import androidx.lifecycle.LifecycleOwner
import lib.chat.model.UploadType
import lib.chat.util.Caching
import lxtx.im.db.Db
import lxtx.im.model.CommonUploadResult
import lxtx.im.util.filetransfer.FileBaseHandler
import lxtx.im.util.filetransfer.upload.multipart.UploadRequest
import lxtx.im.util.filetransfer.upload.multipart.task.FileUploadTask
import vector.EMPTY
import vector.util.FileUtil
import java.io.File

/**
 * @author ningkun
 * @date   2019/12/3
 */
class UploadHandler : FileBaseHandler<UploadRequest>() {

    private var progress: ((Float, Int) -> Unit)? = null
    private var result: ((CommonUploadResult) -> Unit)? = null
    private var error: (() -> Unit)? = null
    private var fileId: ((String) -> Unit)? = null
    private var owner: LifecycleOwner? = null

    fun onProgress(progress: (progress: Float, index: Int) -> Unit) = this.apply { this.progress = progress }

    fun onComplete(result: (uploadResult: CommonUploadResult) -> Unit) = this.apply { this.result = result }

    fun onError(error: () -> Unit) = this.apply { this.error = error }

    fun onFileId(fileId: (fileId: String) -> Unit) = this.apply { this.fileId = fileId }

    fun bindLifecycle(owner: LifecycleOwner? = null) = this.apply { this.owner = owner }

    override fun process(request: UploadRequest) {
        if (request.chunkZipMap.isEmpty()) {//没有要上传的分片，表明上传已经完成
            doCleanUp(request.chunkZipFolderName, request.chunkFolderName)
            buildUploadResult(request.fileId)?.let { result?.invoke(it) }
            return
        }
        var uploadSuccessCount = 0//记录上传成功的次数
        var uploadFailCount = 0//记录上传失败的次数

        for ((index, file) in request.chunkZipMap) {
            FileUploadTask.Builder()
                    .currentPieceNum(index)
                    .currentSize(request.chunkFileLengths[index - 1])
                    .fileName(file.name.substring(0, file.name.length - 1))
                    .filePath(file.path)
                    .fileSuffix(request.suffix)
                    .relateType(UploadType.CHAT)
                    .totalFileSize(request.sourceFileLength)
                    .totalPieces(request.chunkFileLengths.size)
                    .isGenerateLargePicture(request.isGenerateLargePicture)
                    .build().execute().observe {
                        this@UploadHandler.fileId?.invoke(it.fileId)
                        uploadSuccessCount++
                        if (uploadSuccessCount == request.chunkZipMap.size) {//所有的分片上传成功
                            doCleanUp(request.chunkZipFolderName, request.chunkFolderName)
                            //通知所有压缩加密分片上传已经完成了
                            this@UploadHandler.result?.invoke(it)
                        } else if (uploadSuccessCount + uploadFailCount == request.chunkZipMap.size) {//所有的请求都返回了结果，但成功的次数跟请求总数对不上，说明有失败的请求
                            this@UploadHandler.error?.invoke()
                        }
                    }.error {
                        uploadFailCount++
                        if (uploadSuccessCount + uploadFailCount == request.chunkZipMap.size) {
                            this@UploadHandler.error?.invoke()
                        }
                    }.progress { progress, _ ->
                        this@UploadHandler.progress?.invoke(progress, index - 1)
                    }.load(owner)
        }
    }

    /**
     * 删除分片储存目录
     */
    private fun doCleanUp(chunkZipFolderName: String, chunkFolderName: String) {
        FileUtil.delete(Caching.uploadCacheDir.plus(File.separator).plus(chunkZipFolderName))
        FileUtil.delete(Caching.uploadCacheDir.plus(File.separator).plus(chunkFolderName))
    }

    /**
     * 根据本地信息构建上传结果
     */
    private fun buildUploadResult(fileId: String): CommonUploadResult? {
        return Db.sync {
            queryTransferInfo(fileId)
        }?.let { transferInfo ->
            CommonUploadResult(transferInfo.fileId, EMPTY, transferInfo.linkUrl, transferInfo.prefix)
        }
    }
}
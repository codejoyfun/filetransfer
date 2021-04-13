package lxtx.im.util.filetransfer.upload.multipart

import androidx.lifecycle.LifecycleOwner
import lib.chat.ChatBus
import lxtx.im.EventId
import lxtx.im.db.Db
import lxtx.im.model.ChatMessage
import lxtx.im.model.CommonUploadResult
import lxtx.im.model.filetranfer.Transferable
import lxtx.im.model.filetranfer.UploadInfo
import lxtx.im.util.ChatFileUtil
import lxtx.im.util.filetransfer.ChunkOperator
import lxtx.im.util.filetransfer.ProgressCalculator
import lxtx.im.util.filetransfer.upload.FileUploader
import lxtx.im.util.filetransfer.upload.multipart.handler.EncryptCompressHandler
import lxtx.im.util.filetransfer.upload.multipart.handler.RetryHandler
import lxtx.im.util.filetransfer.upload.multipart.handler.SplitHandler
import lxtx.im.util.filetransfer.upload.multipart.handler.UploadHandler
import vector.EMPTY
import vector.appContext
import vector.compat.file.FileCompat
import vector.ext.cast
import java.io.File

/**
 * 文件分片上传
 * @author ningkun
 * @date   2019/11/11
 */
class MultipartUploader(private val transferable: Transferable) : FileUploader() {

    private val chunkFolderName = transferable.obtainChunkFolderName()//文件分片保存目录
    private val chunkZipFolderName = transferable.obtainChunkZipFolderName()//经压缩加密后的文件分片保存目录
    private val lastModifyTime = transferable.obtainLastModifyTime()//文件的最后修改时间

    private val fileTotalLength = transferable.obtainFileTotalLength()//文件的总长度
    private var fileId = transferable.obtainFileId()//文件id，用于文件分片下载
    private val localFilePath = transferable.obtainLocalFilePath()//文件本地路径
    private val suffix = ChatFileUtil.getSuffix(localFilePath)//文件后缀


    private val isGenerateLargePicture: String = "1"//是否生成大图 0：否 1：是 (默认：否)

    override fun execute(owner: LifecycleOwner?) {
        this.owner = owner
        if (checkUpload()) {
            doUpload()
        } else {
            error?.invoke()
        }
    }

    /**
     * 在上传前检查下文件是否存在以及文件夹不允许上传
     */
    private fun checkUpload(): Boolean {
//        val sourceFile = File(localFilePath)
//        return sourceFile.exists() && !sourceFile.isDirectory

        return FileCompat.exists(localFilePath, appContext)
    }

    /**
     * 开始上传，上传分两种情况:1.重试续传 2.从头开始，经历切割,压缩加密处理,最后再上传
     */
    private fun doUpload() {
        when {
            isAlreadyUpload() -> buildUploadResult()?.let { uploadResult?.invoke(it) }//直接把结果返回
            shouldTry() -> retry()
            else -> splitThenZipThenUpload()//默认的处理方式: 分割，压缩加密再到上传
        }
    }

    /**
     * 本地查询数据库，如果能查询到上传成功的记录，证明已经上传过了
     */
    private fun isAlreadyUpload(): Boolean {
        if (fileId.isEmpty()) return false
        return Db.sync {
            queryTransferInfo(fileId) != null
        }
    }

    /**
     * 根据本地信息构建上传结果
     */
    private fun buildUploadResult(): CommonUploadResult? {
        return Db.sync {
            queryTransferInfo(fileId)
        }?.let { transferInfo ->
            CommonUploadResult(transferInfo.fileId, EMPTY, transferInfo.linkUrl, transferInfo.prefix)
        }
    }

    /**
     * 是否需要重试(判断属于重试上传情况的依据：1.文件没有被修改过 2.对应的压缩分片保留完整)
     */
    private fun shouldTry() = !fileIsModified(lastModifyTime, localFilePath) && ChunkOperator.allChunkZipsIsExist(chunkZipFolderName, localFilePath)

    /**
     * 从文件切割开始处理
     */
    private fun splitThenZipThenUpload() {
        doCleanUp()//先清除残留的文件分片和目录
        val request = buildSplitRequest()
        val progressCalculator = ProgressCalculator(request.sourceFileLength)
        SplitHandler()//对源文件进行切块
                .next(customizeEncryptCompressHandler())//对分片加密压缩
                .next(customizeUploadHandler(progressCalculator))//上传分片
                .process(request)
    }

    /**
     * 重试处理完再上传
     */
    private fun retry() {
        val request = buildRetryRequest(ChunkOperator.loadChunksFromHardDisk(chunkFolderName),
                ChunkOperator.loadChunkZipsFromHardDisk(chunkZipFolderName))
        val progressCalculator = ProgressCalculator(request.sourceFileLength)
        customizeRetryHandler(request, progressCalculator)
                .next(customizeUploadHandler(progressCalculator)).process(request)
    }

    /**
     * 构建上传重试处理的请求参数
     */
    private fun buildRetryRequest(chunkFiles: List<File>, chunkZips: List<File>): UploadRequest {
        return UploadRequest().newBuilder()
                .chunkFolderName(chunkFolderName)
                .chunkZipFolderName(chunkZipFolderName)
                .sourceFilePath(localFilePath)
                .chunkFileLengths(chunkFiles.map { it.length() })
                .chunkZipMap(ChunkOperator.transformChunkZipMap(chunkZips))
                .suffix(suffix)
                .sourceFileLength(File(localFilePath).length())
                .isGenerateLargePicture(isGenerateLargePicture)
                .fileId(fileId)
                .build()
    }

    /**
     * 构建从文件切割开始处理的请求参数
     */
    private fun buildSplitRequest() = UploadRequest().newBuilder()
            .sourceFilePath(localFilePath)
            .chunkFolderName(chunkFolderName)
            .chunkZipFolderName(chunkZipFolderName)
            .chunkFileLengths(ChunkOperator.getChunkFileLengths(localFilePath))
            .suffix(suffix)
            .sourceFileLength(File(localFilePath).length())
            .isGenerateLargePicture(isGenerateLargePicture)
            .build()

    /**
     * 配置重试处理器
     */
    private fun customizeRetryHandler(request: UploadRequest, progressCalculator: ProgressCalculator) = RetryHandler()
            .onComplete { uploadedIndexs ->
                progressCalculator.set(getStartProgressLength(request.chunkFileLengths, uploadedIndexs))
            }.onExpire {
                saveFileId(EMPTY)
            }.onError {
                this.error?.invoke()
            }.bindLifecycle(owner)

    /**
     * 配置文件上传处理器
     */
    private fun customizeUploadHandler(progressCalculator: ProgressCalculator) = UploadHandler()
            .onProgress { progressValue, index ->
                progressCalculator.update(progressValue, index)
                progress?.invoke(progressCalculator.get(), fileTotalLength)
                progressCalculator.print(isDownLoad = false)
            }.onComplete { tempUploadResult ->
                saveTransferInfo(tempUploadResult)
                uploadResult?.invoke(tempUploadResult)
            }.onError {
                error?.invoke()
            }.onFileId { chunkFileId ->
                if (!fileIdAlreadySave()) {//只要有一片上传成功,保存fileId,方便以后要重试的时候进行进度查询
                    saveFileId(chunkFileId)
                }
            }.bindLifecycle(owner)

    /**
     * 配置压缩加密处理器
     */
    private fun customizeEncryptCompressHandler() = EncryptCompressHandler().onError { error?.invoke() }

    /**
     * 获取已有的上传进度
     * chunkFileLengths：分片长度集合
     * uploadSuccessIndexs：已经上传成功的文件分片序号
     */
    private fun getStartProgressLength(chunkFileLengths: List<Long>, uploadSuccessIndexs: List<Int>): Long {
        var startProgressLength = 0L
        for ((index, length) in chunkFileLengths.withIndex()) {
            if (uploadSuccessIndexs.contains(index + 1)) {
                startProgressLength += length
            }
        }
        return startProgressLength
    }

    /**
     * 是否已经保存了文件Id
     */
    private fun fileIdAlreadySave() = fileId.isNotEmpty()

    /**
     * 文件内容被修改过
     */
    private fun fileIsModified(lastModifyTime: Long, sourceFilePath: String) = lastModifyTime != 0L && File(sourceFilePath).lastModified() > lastModifyTime

    /**
     * 清空分片目录下的图片
     */
    private fun doCleanUp() {
        File(chunkZipFolderName).listFiles()?.forEach { it.delete() }
        File(chunkFolderName).listFiles()?.forEach { it.delete() }
    }

    /**
     * 上传接口返回FileId时，保存到数据库以及刷新内存中对应的消息体
     */
    private fun saveFileId(fileId: String) {
        this.fileId = fileId
        transferable.updateFileId(fileId)
        transferable.cast<ChatMessage> { message ->
            message.asDbMode()
            Db.sync { updateUploadMessage(message.mid, message.content) }//更新数据库
            ChatBus.get().send(EventId.CHAT_MESSAGE_UPLOAD_FILE_ID, message to fileId)//刷新内存
        }
    }

    /**
     * 上传成功，保存返回来的fileId和url
     */
    private fun saveTransferInfo(uploadResult: CommonUploadResult) {
        Db.async {
            insert(UploadInfo().apply {
                fileId = uploadResult.fileId
                linkUrl = uploadResult.linkUrl
                prefix = uploadResult.prefix
            })
        }
    }

    override fun cancel() {//todo: 取消上传(暂时不需要)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
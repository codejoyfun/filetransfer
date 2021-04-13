package lxtx.im.util.filetransfer.upload.multipart.handler

import lib.chat.util.Caching
import lxtx.im.util.filetransfer.ChunkOperator
import lxtx.im.util.filetransfer.FileBaseHandler
import lxtx.im.util.filetransfer.upload.multipart.UploadRequest
import lxtx.im.util.filetransfer.upload.multipart.task.EncryptCompressTask
import vector.util.FileUtil
import java.io.File

/**
 * 压缩加密分片
 * @author ningkun
 * @date   2019/12/3
 */
class EncryptCompressHandler : FileBaseHandler<UploadRequest>() {

    var error: (() -> Unit)? = null
    fun onError(error: () -> Unit) = this.apply { this.error = error }

    override fun process(request: UploadRequest) {
        //压缩文件分片后的存储目录
        val zipDirectory = createZipDirectory(request.chunkZipFolderName)
        val zipChunkFiles = arrayListOf<File>()//用于存储压缩加密后的文件分片
        var zip4jTaskCount = request.chunkFiles.size//记录当前已经压缩加密完多少分片,以此判断所有分片是否都已经压缩加密完成
        for (chunkFile in request.chunkFiles) {
            //定义压缩加密后的文件名称
            val destFileName = zipDirectory.plus(File.separator).plus(chunkFile.name)
            //开启压缩加密分片的异步任务
            EncryptCompressTask(chunkFile, destFileName).execute { file ->
                file?.let { zipChunkFiles.add(it) }
                if (--zip4jTaskCount == 0) {//所有的压缩加密任务都执行完了
                    if (zipChunkFiles.size != request.chunkFiles.size) {//压缩分片总数和原始分片对不上，说明压缩失败了
                        error?.invoke()
                    } else {
                        zipChunkFiles.sortBy { it.name.last().toInt() }//按序号从1开始排序
                        nextHandler?.process(request.newBuilder()
                                .chunkZipMap(ChunkOperator.transformChunkZipMap(zipChunkFiles))
                                .build())
                    }
                }
            }
        }

    }

    /**
     * 创建用来放置压缩后的分片文件的目录
     */
    private fun createZipDirectory(chunkZipFolderName: String): String {
        val zipDirectory = Caching.uploadCacheDir.plus(File.separator).plus(chunkZipFolderName)
        FileUtil.ensureFileExist(zipDirectory)
        return zipDirectory
    }
}
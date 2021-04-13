package lxtx.im.util.filetransfer.download.multipart

import androidx.lifecycle.LifecycleOwner
import lib.chat.util.Caching.downloadCachePath
import live.Error
import logger.L
import lxtx.im.db.Db
import lxtx.im.model.ChatMessage
import lxtx.im.model.filetranfer.Transferable
import lxtx.im.util.filetransfer.ChunkOperator
import lxtx.im.util.filetransfer.ProgressCalculator
import lxtx.im.util.filetransfer.download.FileDownloader
import lxtx.im.util.filetransfer.download.multipart.handler.DecryptDecompressHandler
import lxtx.im.util.filetransfer.download.multipart.handler.DownloadHandler
import vector.EMPTY
import vector.ext.cast
import vector.ext.runOnMainThread
import vector.ext.runOnSubThread
import vector.util.FileUtil
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 文件分片下载
 * @author ningkun
 * @date   2019/11/26
 */
class MultipartDownloader constructor(private val transferable: Transferable) : FileDownloader() {

    private var fileTotalLength = transferable.obtainFileTotalLength()//文件的总长度
    private var downloadedChunkZipPaths = transferable.obtainDownloadedChunkZipPaths()//已经下载过的文件分块路径集合
    private var fileId = transferable.obtainFileId()//文件id

    private lateinit var indexsNeedDownload: List<Int>//需要下载的压缩加密文件对应的下标(下标从1开始,依照后台的约定)
    private lateinit var unzipFolderPath: String//约定解压解密后存放的文件夹名
    private lateinit var chunkFileLengths: List<Long>//原始分片的文件长度集合
    private lateinit var downloadResultRecord: DownloadResultRecord
    private var chunkCount: Int = 0//文件分片的片数
    private var downloadHandlers = arrayListOf<DownloadHandler>()

    /**
     * 启动下载
     */
    override fun execute(owner: LifecycleOwner?) {
        this.owner = owner
        runOnSubThread {
            preDownload()
            doDownload()
            afterDownload()
        }
    }

    /**
     * 取消下载
     */
    override fun cancel() {
        downloadHandlers.forEach {
            it.cancel()
        }
    }

    /**
     * 在下载之前做些初始化工作
     */
    private fun preDownload() {
        chunkCount = ChunkOperator.calculateChunkCount(fileTotalLength)
        downloadedChunkZipPaths = filterChunkZipPaths(downloadedChunkZipPaths.orEmpty())
        indexsNeedDownload = findChunkIndicesNeedDownload()
        unzipFolderPath = downloadCachePath.plus(File.separator).plus(UUID.randomUUID().toString())
        chunkFileLengths = ChunkOperator.getChunkFileLengths(fileTotalLength)
    }

    /**
     * 把不存在的文件和重复文件去掉
     */
    private fun filterChunkZipPaths(downloadedChunkZipPaths: List<String>) = downloadedChunkZipPaths.distinctBy { it }
            .filter { File(it).exists() && File(it).name.first().toString() != "-" }

    /**
     * 开始下载
     */
    private fun doDownload() {
        val countDownLatch = CountDownLatch(indexsNeedDownload.size)//解压解密的多线程任务计数
        downloadResultRecord = DownloadResultRecord()

        val startProgressLength = getStartProgressLength(chunkFileLengths, indexsNeedDownload)//初始的下载进度
        val progressCalculator = ProgressCalculator(fileTotalLength, startProgressLength)//负责统计下载进度的类

        //开启分片的多任务下载
        for (i in indexsNeedDownload) {
            val request = DownloadRequest().newBuilder()
                    .fileId(fileId)
                    .chunkIndex(i)
                    .unzipFolderPath(unzipFolderPath)
                    .build()
            //依次进行下载和解压处理
            val downloadHandler = customizeDownloadHandler(progressCalculator)
            downloadHandlers.add(downloadHandler)
            downloadHandler.next(DecryptDecompressHandler(countDownLatch, downloadResultRecord))//解压解密
                    .process(request)
        }
        countDownLatch.await()
    }

    private fun customizeDownloadHandler(progressCalculator: ProgressCalculator) = DownloadHandler().onProgress { progress, index ->
        progressCalculator.update(progress, index - 1)//刷新进度
        this.progress?.invoke(progressCalculator.get(), fileTotalLength)
        progressCalculator.print()//打印进度
    }.onComplete { chunkZipPath ->
        updateChunkZipPaths(chunkZipPath)
    }.bindLifecycle(owner)

    /**
     * 下载完成后,比如合并分片，删除临时文件
     */
    private fun afterDownload() {
        if (downloadResultRecord.chunkPaths.size == indexsNeedDownload.size) {//解压的文件分片数量跟分片的上传数量对得上，证明整个解压解密过程成功
            val mergeResultPath = downloadCachePath.plus(File.separator).plus(UUID.randomUUID().toString())
                    .plus(".").plus(downloadResultRecord.suffix)//定义合并文件的路径
            if (ChunkOperator.mergeChunks(unzipFolderPath, mergeResultPath)) {
                deleteTempFiles(downloadedChunkZipPaths.orEmpty(), unzipFolderPath)
                runOnMainThread { this.downloadResult?.invoke(mergeResultPath) }
            }
        } else {
            runOnMainThread { this.error?.invoke(Error(EMPTY, EMPTY)) }
            L.e("解压失败")
        }
    }

    /**
     * 更新数据库,保存已下载的压缩分片的文件路径,方便以后重试下载，已经下载好的分片无需再下
     */
    private fun updateChunkZipPaths(chunkZipPath: String) {
        val paths = ArrayList(downloadedChunkZipPaths.orEmpty())
        paths.add(chunkZipPath)
        transferable.updateDownloadedChunkZipPaths(paths)
        transferable.cast<ChatMessage> { message ->
            message.asDbMode()
            Db.sync {
                updateMessageChunkZipPaths(message.mid, message.content)
            }
        }
    }

    /**
     * 找出需要下载的压缩加密文件序号
     */
    private fun findChunkIndicesNeedDownload(): ArrayList<Int> {
        val alreadyDownloadMap = HashMap<Int, String>()
        for (path in downloadedChunkZipPaths.orEmpty()) {
            alreadyDownloadMap[path.last().toString().toInt()] = path
        }
        val indexsNeedDownload = arrayListOf<Int>()
        for (i in 1..chunkCount) {
            if (!alreadyDownloadMap.contains(i) || !File(alreadyDownloadMap[i]).exists()) {
                indexsNeedDownload.add(i)
            }
        }
        return indexsNeedDownload
    }

    /**
     * 获取已有的下载进度
     */
    private fun getStartProgressLength(chunkFileLengths: List<Long>, indexsNeedDownload: List<Int>): Long {
        var startProgressLength = 0L
        for ((index, length) in chunkFileLengths.withIndex()) {
            if (!indexsNeedDownload.contains(index + 1)) {
                startProgressLength += length
            }
        }
        return startProgressLength
    }

    /**
     * 删除临时文件:之前下载的压缩分片和解压目录
     */
    private fun deleteTempFiles(chunkZipPaths: List<String>, unzipFolderPath: String) {
        for (chunkZipPath in chunkZipPaths) {
            FileUtil.delete(chunkZipPath)
        }
        FileUtil.delete(unzipFolderPath)
    }

    /**
     * 记录文件下载相关信息(主要用来在合并文件成功后加上后缀名，以及删掉下载的分片文件)
     */
    class DownloadResultRecord {
        var suffix = EMPTY//下载文件的后缀名
        val chunkPaths: MutableList<String> = Collections.synchronizedList(ArrayList<String>())//记录解压文件的路径集合

    }
}
package lxtx.im.util.filetransfer

import lib.chat.ext.toInt
import lib.chat.util.Caching
import lxtx.im.ConstantsFileSplit
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * 负责文件分片的通用操作
 * @author ningkun
 * @date   2019/12/18
 */
object ChunkOperator {
    /**
     * 计算文件要分成多少片
     */
    private fun calculateChunkCount(sourceFile: File) = calculateChunkCount(sourceFile.length())

    /**
     * 计算文件要分成多少片
     */
    fun calculateChunkCount(fileTotalLength: Long): Int {
        var chunkCount = fileTotalLength / ConstantsFileSplit.CHUNK_SIZE
        if (fileTotalLength % ConstantsFileSplit.CHUNK_SIZE != 0L) {
            chunkCount++
        }
        return chunkCount.toInt()
    }

    /**
     * 获取原始分片的长度集合 如13M的源文件，长度集合为(5,5,3),用于上传接口的请求参数或进度计算
     */
    fun getChunkFileLengths(sourceFilePath: String) = getChunkFileLengths(File(sourceFilePath).length())

    /**
     * 获取原始分片的长度集合 如13M的源文件，长度集合为(5,5,3),用于上传接口的请求参数或进度计算
     */
    fun getChunkFileLengths(fileTotalLength: Long): List<Long> {
        var chunkCount = fileTotalLength / ConstantsFileSplit.CHUNK_SIZE
        val chunkFileLengths = arrayListOf<Long>()
        for (i in 1..chunkCount) {
            chunkFileLengths.add(ConstantsFileSplit.CHUNK_SIZE)
        }
        if (fileTotalLength % ConstantsFileSplit.CHUNK_SIZE != 0L) {
            chunkFileLengths.add(fileTotalLength % ConstantsFileSplit.CHUNK_SIZE)
        }
        return chunkFileLengths
    }

    /**
     * 是否已经存在压缩加密文件分片(注意是所有压缩加密文件分片都存在)
     */
    fun allChunkZipsIsExist(chunkZipFolderName: String, sourceFilePath: String): Boolean {
        if (chunkZipFolderName.isEmpty()) return false
        val chunkZipFolder = File(Caching.uploadCacheDir.plus(File.separator).plus(chunkZipFolderName))
        return chunkZipFolder.exists() && chunkZipFolder.list().size == calculateChunkCount(File(sourceFilePath))
    }

    /**
     * 从硬盘里加载同所有属一个源文件,经压缩加密过的文件分片
     */
    fun loadChunkZipsFromHardDisk(chunkZipFolderName: String): List<File> {
        val chunkZips = File(Caching.uploadCacheDir.plus(File.separator).plus(chunkZipFolderName)).listFiles().toList()
        chunkZips.sortedBy { it.name.last().toInt() }
        return chunkZips
    }

    /**
     * 从硬盘里加载同所有属一个源文件,原始的文件分片
     */
    fun loadChunksFromHardDisk(chunkFolderName: String): List<File> {
        val chunkFiles = File(Caching.uploadCacheDir.plus(File.separator).plus(chunkFolderName)).listFiles().toList()
        chunkFiles.sortedBy { it.name.last().toInt() }
        return chunkFiles
    }

    /**
     * 转成map key:分片序号 value:压缩分片
     */
    fun transformChunkZipMap(chunkZips: List<File>): Map<Int, File> {
        val chunkZipMap = HashMap<Int, File>()
        for (zip in chunkZips) {
            chunkZipMap[zip.name.lastOrNull()?.toString().toInt()] = zip
        }
        return chunkZipMap
    }

    /**
     * 合并分片
     * @param chunkFolderPath:存在分片的文件夹路径
     * @param mergeResultPath:合并分片生成的文件路径
     */
    fun mergeChunks(chunkFolderPath: String, mergeResultPath: String): Boolean {
        var chunks = File(chunkFolderPath).listFiles()?.toList().orEmpty()
        if (chunks.isEmpty()) return false
        //只有一个分片，不需要进行合并
        if (chunks.size == 1) {
            chunks[0].renameTo(File(mergeResultPath))
            return true
        }
        chunks = chunks.sortedBy { it.name.last().toInt() }//按字符的顺序也对
        val resultFile = File(mergeResultPath)
        var resultFileChannel: FileChannel? = null
        var chunkChannel: FileChannel? = null
        try {
            resultFileChannel = FileOutputStream(resultFile, true).channel
            for (chunk in chunks) {
                chunkChannel = FileInputStream(chunk).channel
                resultFileChannel.transferFrom(chunkChannel, resultFileChannel.size(), chunkChannel.size())
                chunkChannel.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            chunkChannel?.close()
            resultFileChannel?.close()
        }
        return true
    }
}
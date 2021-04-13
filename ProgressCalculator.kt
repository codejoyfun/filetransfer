package lxtx.im.util.filetransfer

import logger.L

/**
 * 上传或下载进度计算
 * @author ningkun
 * @date   2019/12/9
 */
class ProgressCalculator(private var sourceFileLength: Long, private var startProgressLength: Long = 0L) {//初始化已经上传或下载的进度

    private val progressMap = HashMap<Int, Long>()//记录每个分块的进度
    private var chunkFileLengths = ChunkOperator.getChunkFileLengths(sourceFileLength)//文件分片的文件长度集合

    fun set(startProgressLength: Long) {
        this.startProgressLength = startProgressLength
    }

    /**
     * 更新进度
     */
    fun update(progressValue: Float, index: Int) {
        progressMap[index] = (chunkFileLengths[index] * progressValue / 100f).toLong()
    }

    /**
     * 获取目前已上传或已下载的文件长度
     */
    private fun getContentLength(): Long {
        var totalLength = startProgressLength
        for (entry in progressMap) {
            totalLength += entry.value
        }
        return totalLength
    }

    /**
     * 获取当前进度值(取值范围:[0,1])
     */
    fun get() = getContentLength() / sourceFileLength.toFloat()

    fun print(isDownLoad: Boolean = true) {
        if (isDownLoad) {
            L.i("文件分片下载进度".plus(get()))
        } else {
            L.i("文件分片上传进度".plus(get()))
        }
    }

}
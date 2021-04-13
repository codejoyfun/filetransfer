package lxtx.im.util.filetransfer.upload.multipart.handler

import lib.chat.util.Caching
import lxtx.im.util.filetransfer.ChunkOperator
import lxtx.im.util.filetransfer.FileBaseHandler
import lxtx.im.util.filetransfer.upload.multipart.UploadRequest
import vector.util.FileUtil
import java.io.File
import java.io.RandomAccessFile
import java.util.*

/**
 * 切割文件(默认5M为一个分片)
 * @author ningkun
 * @date   2019/12/3
 */
class SplitHandler : FileBaseHandler<UploadRequest>() {

    private lateinit var sourceFile: RandomAccessFile
    private lateinit var buffer: ByteArray
    private lateinit var chunkDirectory: String
    private val chunkFiles = arrayListOf<File>()//文件分片集合
    //注意一点:所有的分片命名除了最后的一个字符表示序号不同以外，其他相同，比如第一个分片的名字：aaa1,第二个就为：aaa2，以此类推
    private val chunkFileNamePrefix = removeHorizontalLine(UUID.randomUUID().toString())//分片名字的前缀

    /**
     * 文件分片预处理
     */
    private fun preSplit(request: UploadRequest) {
        sourceFile = RandomAccessFile(request.sourceFilePath, "rw")
        //用于读取文件分片的字节缓冲区
        buffer = ByteArray(request.chunkSize.toInt())
        //文件分片的存储目录
        chunkDirectory = getChunkDirectory(request)
    }


    override fun process(request: UploadRequest) {
        preSplit(request)
        ///////////////////////////////分片处理//////////////////////////////////////////////////
        var serialNumber = 0//按顺序给每个文件分片的文件名追加序号，例如第一个文件分片追加1
        while (true) {
            var currentChunkSize = sourceFile.read(buffer)//记录当前文件分片的大小
            if (currentChunkSize == -1) break //源文件已经读取完了,不需要再处理
            chunkFiles.add(generateChunk(currentChunkSize, chunkFileNamePrefix.plus(++serialNumber)))//序列号自增
        }
        //进入下一个处理环节
        nextHandler?.process(request.newBuilder()
                .chunkFiles(chunkFiles)
                .chunkFileLengths(ChunkOperator.getChunkFileLengths(request.sourceFilePath))//未经压缩解密的分片文件大小集合
                .build())
    }

    /**
     * 生成分片，往分片写入内容
     */
    private fun generateChunk(currentChunkSize: Int, chunkFileName: String): File {
        val chunkFile = File(chunkDirectory, chunkFileName)
        //往分片写入内容
        val writeFile = RandomAccessFile(chunkFile, "rw")
        writeFile.write(buffer, 0, currentChunkSize)
        writeFile.close()
        return chunkFile
    }

    /**
     * 获取分片目录
     */
    private fun getChunkDirectory(request: UploadRequest): String {
        //文件分片的存储目录
        val parent = Caching.uploadCacheDir.plus(File.separator).plus(request.chunkFolderName)
        //如果存储目录不存在,新建目录
        FileUtil.ensureFileExist(parent)
        return parent
    }

    private fun removeHorizontalLine(uuid: String) = uuid.replace("-", "")
}
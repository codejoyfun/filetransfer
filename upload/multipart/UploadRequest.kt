package lxtx.im.util.filetransfer.upload.multipart

import lxtx.im.ConstantsFileSplit
import vector.EMPTY
import java.io.File

/**
 * @author ningkun
 * @date   2019/12/3
 */
class UploadRequest internal constructor() {

    var chunkFolderName: String = EMPTY
        private set

    var chunkZipFolderName: String = EMPTY
        private set

    var sourceFilePath: String = EMPTY
        private set

    var chunkSize: Long = ConstantsFileSplit.CHUNK_SIZE
        private set

    var chunkFiles: List<File> = arrayListOf()
        private set

    var chunkFileLengths: List<Long> = arrayListOf()
        private set

    var chunkZipMap: Map<Int, File> = HashMap()
        private set

    var suffix: String = EMPTY
        private set

    var sourceFileLength: Long = 0L
        private set

    var fileId: String = EMPTY
        private set

    var isGenerateLargePicture: String = EMPTY 	//是否生成大图 0：否 1：是 (默认：否)
        private set

    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder {
        //文件分片存放的位置
        private var chunkFolderName: String = EMPTY
        //文件分片压缩后存放的位置
        private var chunkZipFolderName: String = EMPTY
        //源文件路径
        private var sourceFilePath: String = EMPTY
        //文件切割的大小
        private var chunkSize: Long = ConstantsFileSplit.CHUNK_SIZE
        //文件分片集合
        private var chunkFiles: List<File> = arrayListOf()
        //文件分片的文件长度集合
        private var chunkFileLengths: List<Long> = arrayListOf()
        //压缩加密后的文件分片集合 key:文件分片序号 value:压缩分片文件
        private var chunkZipMap: Map<Int, File> = HashMap()
        //源文件的后缀名
        private var suffix: String = EMPTY
        //源文件的文件长度
        private var sourceFileLength: Long = 0L
        //服务器对应的文件id
        private var fileId: String = EMPTY
        //是否生成大图 0：否 1：是 (默认：否)
        private var isGenerateLargePicture: String = EMPTY

        internal constructor(request: UploadRequest) {
            chunkFolderName = request.chunkFolderName
            chunkZipFolderName = request.chunkZipFolderName
            sourceFilePath = request.sourceFilePath

            chunkSize = request.chunkSize
            chunkFiles = request.chunkFiles
            chunkFileLengths = request.chunkFileLengths

            chunkZipMap = request.chunkZipMap
            suffix = request.suffix
            sourceFileLength = request.sourceFileLength

            fileId = request.fileId
            isGenerateLargePicture = request.isGenerateLargePicture
        }

        fun chunkFolderName(chunkFolderName: String): Builder {
            this.chunkFolderName = chunkFolderName
            return this
        }

        fun chunkZipFolderName(chunkZipFolderName: String): Builder {
            this.chunkZipFolderName = chunkZipFolderName
            return this
        }

        fun sourceFilePath(sourceFilePath: String): Builder {
            this.sourceFilePath = sourceFilePath
            return this
        }

        fun chunkSize(chunkSize: Long): Builder {
            this.chunkSize = chunkSize
            return this
        }

        fun chunkFiles(chunkFiles: List<File>): Builder {
            this.chunkFiles = chunkFiles
            return this
        }

        fun chunkFileLengths(chunkFileLengths: List<Long>): Builder {
            this.chunkFileLengths = chunkFileLengths
            return this
        }

        fun chunkZipMap(chunkZipMap: Map<Int, File>): Builder {
            this.chunkZipMap = chunkZipMap
            return this
        }

        fun suffix(suffix: String): Builder {
            this.suffix = suffix
            return this
        }

        fun sourceFileLength(sourceFileLength: Long): Builder {
            this.sourceFileLength = sourceFileLength
            return this
        }

        fun fileId(fileId: String): Builder {
            this.fileId = fileId
            return this
        }

        fun isGenerateLargePicture(isGenerateLargePicture: String): Builder {
            this.isGenerateLargePicture = isGenerateLargePicture
            return this
        }

        fun build(): UploadRequest {
            return UploadRequest().let {
                it.chunkFolderName = chunkFolderName
                it.sourceFilePath = sourceFilePath
                it.chunkSize = chunkSize

                it.chunkFiles = chunkFiles
                it.chunkFileLengths = chunkFileLengths
                it.chunkZipFolderName = chunkZipFolderName

                it.chunkZipMap = chunkZipMap
                it.suffix = suffix
                it.sourceFileLength = sourceFileLength

                it.fileId = fileId
                it.isGenerateLargePicture = isGenerateLargePicture

                it
            }
        }
    }

}
package lxtx.im.util.filetransfer.upload.multipart.task

import lxtx.im.util.filetransfer.upload.multipart.FileRepo
import vector.EMPTY

/**
 * @author ningkun
 * @date   2019/11/12
 */
class FileUploadTask private constructor(val builder: Builder) {

    private val repo = FileRepo()

    fun execute() = repo.upload(builder.filePath, builder.totalFileSize, builder.currentSize, builder.totalPieces, builder.currentPieceNum,
            builder.relateType, builder.fileSuffix, builder.fileName, builder.isGenerateLargePicture)

    class Builder {
        internal var filePath = EMPTY
        internal var totalFileSize = 0L
        internal var currentSize = 0L

        internal var totalPieces = 0
        internal var currentPieceNum = 0
        internal var relateType = EMPTY

        internal var fileSuffix = EMPTY
        internal var fileName = EMPTY
        internal var isGenerateLargePicture = EMPTY

        fun filePath(filePath: String): Builder {
            this.filePath = filePath
            return this
        }

        fun totalFileSize(totalFileSize: Long): Builder {
            this.totalFileSize = totalFileSize
            return this
        }

        fun currentSize(currentSize: Long): Builder {
            this.currentSize = currentSize
            return this
        }

        fun totalPieces(totalPieces: Int): Builder {
            this.totalPieces = totalPieces
            return this
        }

        fun currentPieceNum(currentPieceNum: Int): Builder {
            this.currentPieceNum = currentPieceNum
            return this
        }

        fun relateType(relateType: String): Builder {
            this.relateType = relateType
            return this
        }

        fun fileSuffix(fileSuffix: String): Builder {
            this.fileSuffix = fileSuffix
            return this
        }

        fun fileName(fileName: String): Builder {
            this.fileName = fileName
            return this
        }

        fun isGenerateLargePicture(isGenerateLargePicture: String): Builder {
            this.isGenerateLargePicture = isGenerateLargePicture
            return this
        }

        fun build(): FileUploadTask {
            return FileUploadTask(this)
        }
    }
}
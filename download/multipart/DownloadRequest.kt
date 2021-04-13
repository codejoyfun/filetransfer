package lxtx.im.util.filetransfer.download.multipart

import vector.EMPTY

/**
 * @author ningkun
 * @date   2019/12/4
 */
class DownloadRequest internal constructor() {

    var downloadPath: String = EMPTY
        private set

    var unzipFolderPath: String = EMPTY
        private set

    var fileId: String = EMPTY
        private set

    var chunkIndex: Int = 0
        private set

    var suffix: String = EMPTY
        private set

    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder {
        //下载文件的路径
        private var downloadPath: String = EMPTY
        //解压解密文件的路径
        private var unzipFolderPath: String = EMPTY
        //文件id
        private var fileId: String = EMPTY
        //第几片
        var chunkIndex: Int = 0
        //文件后缀
        private var suffix: String = EMPTY

        internal constructor(request: DownloadRequest) {
            downloadPath = request.downloadPath
            unzipFolderPath = request.unzipFolderPath
            fileId = request.fileId
            chunkIndex = request.chunkIndex
            suffix = request.suffix
        }

        fun downloadPath(downloadPath: String): Builder {
            this.downloadPath = downloadPath
            return this
        }

        fun unzipFolderPath(unzipFolderPath: String): Builder {
            this.unzipFolderPath = unzipFolderPath
            return this
        }

        fun fileId(fileId: String): Builder {
            this.fileId = fileId
            return this
        }

        fun chunkIndex(chunkIndex: Int): Builder {
            this.chunkIndex = chunkIndex
            return this
        }

        fun suffix(suffix: String): Builder {
            this.suffix = suffix
            return this
        }

        fun build(): DownloadRequest {
            return DownloadRequest().let {
                it.downloadPath = downloadPath
                it.unzipFolderPath = unzipFolderPath
                it.fileId = fileId

                it.chunkIndex = chunkIndex
                it.suffix = suffix
                it
            }
        }
    }
}
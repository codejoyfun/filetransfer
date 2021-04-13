package lxtx.im.util.filetransfer.upload.multipart

import eth.toLive
import lib.chat.net.fileNet
import lxtx.im.CommonApi

/**
 * @author ningkun
 * @date   2019/11/25
 */
class FileRepo {

    fun upload(filePath: String, totalFileSize: Long, currentSize: Long, totalPieces: Int, currentPieceNum: Int,
               relateType: String, fileSuffix: String, fileName: String, isGenerateLargePicture: String) = fileNet.create(CommonApi::class)
            .multipartUpload(filePath, totalFileSize, currentSize, totalPieces, currentPieceNum,
                    relateType, fileSuffix, fileName, isGenerateLargePicture = isGenerateLargePicture).toLive()

    fun getUploadProgress(fileId: String) = fileNet.create(CommonApi::class)
            .getUploadProgress(fileId).toLive()

    fun download(fileId: String, fileNumber: Int) = fileNet.create(CommonApi::class)
            .multipartDownloadFile(fileId, fileNumber).toLive()
}
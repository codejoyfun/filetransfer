package lxtx.im.util.filetransfer.download.multipart.handler

import lib.chat.Zip4j
import lib.chat.util.SignUtils
import lxtx.im.util.filetransfer.FileBaseHandler
import lxtx.im.util.filetransfer.download.multipart.DownloadRequest
import lxtx.im.util.filetransfer.download.multipart.MultipartDownloader
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import vector.ext.runOnSubThread
import vector.util.FileUtil
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * 对下载回来的文件分片进行解压解密处理
 * @author ningkun
 * @date   2019/12/4
 */
class DecryptDecompressHandler(private val countDownLatch: CountDownLatch,
                               private var downloadResultRecord: MultipartDownloader.DownloadResultRecord) : FileBaseHandler<DownloadRequest>() {

    override fun process(request: DownloadRequest) {
        runOnSubThread {
            var zipSourceFile: File? = null
            try {
                downloadResultRecord.suffix = request.suffix
                zipSourceFile = File(request.downloadPath)
                val zipFile = ZipFile(zipSourceFile)
                FileUtil.ensureFileExist(File(request.unzipFolderPath))
                zipFile.setFileNameCharset("GBK") //设置编码格式（支持中文）
                if (!zipFile.isValidZipFile) {//检查输入的zip文件是否是有效的zip文件
                    throw ZipException("压缩文件不合法,可能被损坏.")
                }
                if (zipFile.isEncrypted) {
                    zipFile.setPassword(generatePassword(zipSourceFile.name).toCharArray())
                }
                zipFile.extractAll(request.unzipFolderPath)
                downloadResultRecord.chunkPaths.add(request.unzipFolderPath.plus(zipSourceFile.name))//记录解压好的分片路径
            } catch (e: ZipException) {
                e.printStackTrace()
                //解压失败,这个分片可能下载不完整，删掉，重新下载
                zipSourceFile?.delete()
            } finally {
                countDownLatch.countDown()
            }
        }
    }

    /**
     * 生成解压密码
     */
    private fun generatePassword(fileName: String) = SignUtils.encode(SignUtils.encode(fileName.substring(0, fileName.length - 1).plus(Zip4j.VERIFY_PASSWORD_KEY)))
}
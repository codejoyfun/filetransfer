package lxtx.im.util.filetransfer.upload.multipart.task

import lib.chat.Zip4j.VERIFY_PASSWORD_KEY
import lib.chat.util.SignUtils
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.util.Zip4jConstants
import vector.ext.runOnMainThread
import vector.ext.runOnSubThread
import java.io.File

/**
 * 用zip4jTask压缩文件
 * @author ningkun
 * @date   2019/11/21
 */
class EncryptCompressTask constructor(private val sourceFile: File, private val destFileName: String) {

    fun execute(callBack: (file: File?) -> Unit) {
        runOnSubThread {
            val destFile = File(destFileName)
            try {
                //创建压缩文件
                val respFile = ZipFile(destFile)
                //设置压缩文件参数
                val params = configureParams(destFile)
                //添加文件到压缩文件
                respFile.addFile(sourceFile, params)
                runOnMainThread { callBack.invoke(respFile.file) }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnMainThread { callBack.invoke(null) }//解压失败
            }
        }
    }

    private fun configureParams(destFile: File): ZipParameters {
        //设置压缩文件参数
        val params = ZipParameters()
        params.compressionMethod = Zip4jConstants.COMP_DEFLATE
        //设置压缩级别
        params.compressionLevel = Zip4jConstants.DEFLATE_LEVEL_ULTRA
        //设置压缩文件加密
        params.isEncryptFiles = true
        //设置加密方法
        params.encryptionMethod = Zip4jConstants.ENC_METHOD_AES
        //设置aes加密强度
        params.aesKeyStrength = Zip4jConstants.AES_STRENGTH_128
        //设置密码
        params.setPassword(generatePassword(destFile.name))
        return params
    }

    private fun generatePassword(fileName: String) = SignUtils.encode(SignUtils.encode(fileName.substring(0, fileName.length - 1).plus(VERIFY_PASSWORD_KEY)))

}
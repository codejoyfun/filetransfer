package lxtx.im.util.filetransfer.imagegenerator

import lib.chat.Constants
import lxtx.im.model.ChatMessageUploadResult
import vector.EMPTY
import java.util.*

/**
 * @author 宁锟
 * @since 2020/3/25
 */
class FincyImageGenerator(private val result: ChatMessageUploadResult) : ImageGenerator {

    companion object {
        private const val CDN_HOST = "cdnfile"//cdn域名，缩略图拼接规则失效,只有大图
    }

    override fun getThumbPath(): String {
        return when {
            result.host.orEmpty().contains(CDN_HOST) -> result.path.orEmpty()
            isGif() -> result.path.orEmpty()
            else -> {
                result.path?.let {
                    val fileNameIndex = it.lastIndexOf("/")
                    val fileName = it.substring(fileNameIndex + 1)
                    val preFix = it.substring(0, fileNameIndex + 1)
                    preFix.plus("thumb_").plus(fileName)
                }.orEmpty()
            }
        }
    }

    override fun getBigImagePath(): String {
        return if (isGif() || result.host.orEmpty().contains(CDN_HOST)) {
            //gif图不生成大图
            result.path.orEmpty()
        } else {
            result.path?.let {
                val fileNameIndex = it.lastIndexOf("/")
                val fileName = it.substring(fileNameIndex + 1)
                val preFix = it.substring(0, fileNameIndex + 1)
                preFix.plus("big_").plus(fileName)
            }.orEmpty()
        }
    }

    override fun getThumbUrl(): String {
        return result.host.plus(getThumbPath())
    }

    override fun getBigImageUrl(): String {
        return result.host.plus(getBigImagePath())
    }

    private fun isGif(): Boolean {
        val format = result.path?.let { path ->
            path.substring(path.lastIndexOf(".") + 1)
        } ?: EMPTY
        return format.toLowerCase(Locale.getDefault()) == Constants.GIF
    }

}
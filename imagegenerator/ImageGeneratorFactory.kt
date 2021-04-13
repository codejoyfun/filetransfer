package lxtx.im.util.filetransfer.imagegenerator

import lib.chat.Constants.IMAGE_HOST
import lib.chat.constants.AwsConstants.AWS_HOST
import lxtx.im.model.ChatMessage
import lxtx.im.model.ChatMessageUploadResult
import lxtx.im.model.CommonUploadResult
import lxtx.im.util.filetransfer.QiNiuYunUtil
import java.net.URI

/**
 * 缩略图，大图生成规则工厂类
 * @author 宁锟
 * @since 2020/3/25
 */
object ImageGeneratorFactory {

    private const val OLD_HOST = "https://file.6xprog.com/" //历史久远的图片域名

    private val EMPTY_CHAT_MESSAGE = ChatMessage()

    fun get(result: ChatMessageUploadResult): ImageGenerator {
        return when {
            result.host == AWS_HOST -> AmazonImageGeneratorFactory(result)
            result.host == OLD_HOST -> FincyImageGenerator(result)
            result.host == IMAGE_HOST -> FincyImageGenerator(result)
            QiNiuYunUtil.isQiNiuYunUrl(result.path.orEmpty()) -> QiNiuYunImageGenerator(result)
            else -> DefaultImageGenerator(result)
        }
    }

    fun get(result: CommonUploadResult): ImageGenerator {
        val realResult = ChatMessageUploadResult(result.prefix, result.linkUrl, EMPTY_CHAT_MESSAGE)
        return get(realResult)
    }

    fun get(url: String): ImageGenerator {
        val uri = URI(url)
        var path = uri.path
        if (path.isNotEmpty() && path.first() == '/') path = path.substring(1, path.length)
        val result = ChatMessageUploadResult(url.substring(0, url.length - path.length), path, EMPTY_CHAT_MESSAGE)
        return get(result)
    }
}
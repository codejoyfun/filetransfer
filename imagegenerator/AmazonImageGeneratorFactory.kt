package lxtx.im.util.filetransfer.imagegenerator

import lib.chat.constants.AwsConstants.AWS_HOST_BIG
import lib.chat.constants.AwsConstants.AWS_HOST_THUMB
import lxtx.im.model.ChatMessageUploadResult

/**
 * 亚马逊
 * @author 宁锟
 * @since 2020/3/31
 */
class AmazonImageGeneratorFactory(private val result: ChatMessageUploadResult) : ImageGenerator {
    override fun getThumbUrl(): String {
        return AWS_HOST_THUMB.plus(result.path)
    }

    override fun getBigImageUrl(): String {
        return AWS_HOST_BIG.plus(result.path)
    }

    override fun getThumbPath(): String {
        return result.path.orEmpty()
    }

    override fun getBigImagePath(): String {
        return result.path.orEmpty()
    }
}
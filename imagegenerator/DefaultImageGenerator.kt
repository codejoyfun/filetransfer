package lxtx.im.util.filetransfer.imagegenerator

import lxtx.im.model.ChatMessageUploadResult

/**
 * 默认不进行任何规则拼接
 * @author 宁锟
 * @since 2020/4/1
 */
class DefaultImageGenerator(private val result: ChatMessageUploadResult) : ImageGenerator {
    override fun getThumbPath(): String {
        return result.path.orEmpty()
    }

    override fun getBigImagePath(): String {
        return result.path.orEmpty()
    }

    override fun getThumbUrl(): String {
        return result.host.plus(result.path)
    }

    override fun getBigImageUrl(): String {
        return result.host.plus(result.path)
    }
}
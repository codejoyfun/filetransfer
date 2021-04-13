package lxtx.im.util.filetransfer.imagegenerator

import lxtx.im.model.ChatMessageUploadResult

/**
 * @author 宁锟
 * @since 2020/3/25
 */
class QiNiuYunImageGenerator(private val result: ChatMessageUploadResult) : ImageGenerator {

    companion object {
        const val SUFFIX_THUMB = "@thumb"
        const val SUFFIX_BIG_IMAGE = "@big"
    }

    override fun getThumbPath(): String {
        return result.path.plus(SUFFIX_THUMB)
    }

    override fun getBigImagePath(): String {
        return result.path.plus(SUFFIX_BIG_IMAGE)
    }

    override fun getThumbUrl(): String {
        return result.host.plus(getThumbPath())
    }

    override fun getBigImageUrl(): String {
        return result.host.plus(getBigImagePath())
    }


}
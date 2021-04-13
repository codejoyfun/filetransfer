package lxtx.im.util.filetransfer.imagegenerator

/**
 * 缩略图，大图拼接策略
 * @author 宁锟
 * @since 2020/3/25
 */
interface ImageGenerator {
    fun getThumbUrl(): String//缩略图
    fun getBigImageUrl(): String//大图

    fun getThumbPath(): String//缩略图相对路径
    fun getBigImagePath(): String//大图相对路径
}
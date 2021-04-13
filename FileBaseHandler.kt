package lxtx.im.util.filetransfer

/**
 * @author ningkun
 * @date   2019/12/3
 * 文件分片抽象处理类
 */
abstract class FileBaseHandler<T> {
    private val chain = arrayListOf<FileBaseHandler<T>>() //记录处理链,不包括自身
    protected var nextHandler: FileBaseHandler<T>? = null
        get() = chain.first()

    fun next(nextHandler: FileBaseHandler<T>): FileBaseHandler<T> {
        chain.takeIf { it.isNotEmpty() }?.last()?.next(nextHandler)//记住下一个处理器 如：A->B->C  那A的nextHandler是B, B的nextHandler是C
        chain.add(nextHandler)
        return this
    }

    abstract fun process(request: T)//处理函数
}
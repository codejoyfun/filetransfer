package lxtx.im.util.filetransfer.upload.multipart.handler

import androidx.lifecycle.LifecycleOwner
import logger.L
import lxtx.im.util.filetransfer.FileBaseHandler
import lxtx.im.util.filetransfer.upload.multipart.FileRepo
import lxtx.im.util.filetransfer.upload.multipart.UploadRequest
import java.io.File

/**
 * @author ningkun
 * @date   2019/12/3
 */
class RetryHandler : FileBaseHandler<UploadRequest>() {

    private val repo = FileRepo()

    private var result: ((List<Int>) -> Unit)? = null
    private var error: (() -> Unit)? = null
    private var expire: (() -> Unit)? = null
    private var owner: LifecycleOwner? = null

    fun onComplete(result: (uploadedIndexs: List<Int>) -> Unit) = this.apply { this.result = result }

    fun onError(error: () -> Unit) = this.apply { this.error = error }

    fun onExpire(expire: () -> Unit) = this.apply { this.expire = expire }

    fun bindLifecycle(owner: LifecycleOwner? = null) = this.apply { this.owner = owner }

    override fun process(request: UploadRequest) {
        //调用接口获取上传进度(重试上传是必须调用接口，因为临时的文件分片在服务器只保留3小时，要去检查分片是否已经过期)
        if (request.fileId.isNotEmpty()) {
            repo.getUploadProgress(request.fileId).observe { processResult ->
                val uploadedIndexs = processResult.list.orEmpty()
                if(uploadedIndexs.isEmpty()){//没有任何分片上传成功,证明分片由于过期，服务器已经删除，此时本地的fileId无效,fileId需要置空
                    this@RetryHandler.expire?.invoke()
                }else{
                    this@RetryHandler.result?.invoke(uploadedIndexs)//更新已有进度
                }
                processInternal(request, uploadedIndexs)//继续下一个环节的处理
            }.error {
                this@RetryHandler.error?.invoke()
                L.e("获取上传进度失败")
            }.load(owner)
        } else {
            processInternal(request)
        }
    }

    private fun processInternal(request: UploadRequest, uploadedIndexs: List<Int>? = null) {
        nextHandler?.process(request.newBuilder()
                .chunkZipMap(findChunkZipsNeedUpload(request.chunkZipMap, uploadedIndexs.orEmpty()))//找出上传失败或尚未上传的压缩解密分片集合
                .build())
    }

    /**
     * 收集之前还没上传过或上传失败的文件分片来重新上传,注意:分片的文件名最后一个字符约定为分片的序号
     */
    private fun findChunkZipsNeedUpload(chunkZipMap: Map<Int,File>, uploadSuccessIndexs: List<Int>): Map<Int, File> {
        val newChunkZipMap = HashMap<Int,File>()
        for (zip in chunkZipMap) {
            if (!uploadSuccessIndexs.contains(zip.value.name.lastOrNull()?.toString()?.toInt())) {
                newChunkZipMap[zip.key] = zip.value
            }
        }
        return newChunkZipMap
    }
}
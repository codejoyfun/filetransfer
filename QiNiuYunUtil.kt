package lxtx.im.util.filetransfer

import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import eth.toLive
import lib.chat.model.Time
import lib.chat.net.NET
import live.ActionLive
import live.Live
import logger.L
import lxtx.im.CommonApi
import lxtx.im.model.QiNiuYunDeadline
import lxtx.im.model.QiuNiuYunConfig
import lxtx.im.util.Base64
import java.net.URL


/**
 * 获取并缓存七牛云的下载域名，token
 * @author 宁锟
 * @since 2020/3/26
 */
object QiNiuYunUtil {
    private val liveQiuNiuYunConfig = Live<QiuNiuYunConfig>()
    private var config: QiuNiuYunConfig? = null
    private const val timeOffset = 5 * 60 //提前5分钟
    private const val PATH_PATTERN = "qiniu"

    fun getConfig(owner: LifecycleOwner?, block: ((config: QiuNiuYunConfig) -> Unit)? = null, error: (() -> Unit)? = null) {
        if (checkConfig()) {
            config?.let { block?.invoke(it) }
        } else {
            getQiuNiuYunConfig().observe(owner) {
                config = it
                block?.invoke(it)
            }.error {
                error?.invoke()
                L.e(it.message)
            }
        }
    }

    /**
     * 如果是七牛云的链接,就更换自己所在地区的域名
     */
    fun adjustUrl(url: String): String {
        return if (isQiNiuYunUrl(url) && config?.downloadHost != null) {
            var path = URL(url).path
            if (path.isNotEmpty() && path.first() == '/') path = path.substring(1, path.length)
            val realDownloadHost = if (config?.downloadHost?.last() == '/') config?.downloadHost else config?.downloadHost?.plus("/")
            realDownloadHost.plus(path)
        } else {
            url
        }
    }

    fun adjustHost(originalHost: String): String {
        return config?.downloadHost?.let { host ->
            if (host.last() == '/') host else host.plus("/")
        } ?: originalHost
    }

    fun isQiNiuYunUrl(url: String) = url.contains(PATH_PATTERN)

    private fun checkConfig(): Boolean {
        if (config == null) {
            return false
        } else {
            config?.let {
                //{"scope":"fincyhd","deadline":1585208574}
                val upTokenJsonStr = String(Base64.getDecoder().decode(it.upToken.substring(it.upToken.lastIndexOf(":") + 1)))
                val upTokenTime = Gson().fromJson(upTokenJsonStr, QiNiuYunDeadline::class.java)
                return upTokenTime.deadline - timeOffset > Time.serviceTime / 1000
            }
            return false
        }
    }

    private fun getQiuNiuYunConfig(): ActionLive<QiuNiuYunConfig> {
        return liveQiuNiuYunConfig.execute(NET.create(CommonApi::class).getQiuNiuYunConfig().toLive())
    }
}
package lxtx.im.util.filetransfer.download

import androidx.lifecycle.LifecycleOwner

class EmptyDownloader : FileDownloader() {
    override fun execute(owner: LifecycleOwner?) {
        // do nothing
    }

    override fun cancel() {
        //do nothing
    }
}
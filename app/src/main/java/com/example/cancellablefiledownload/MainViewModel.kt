package com.example.cancellablefiledownload

import android.app.Application
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cancellablefiledownload.ui.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.*
import java.io.*
import java.net.URL
import java.util.*


class MainViewModel(val app: Application) : AndroidViewModel(app) {
    var urlString = ""

    private val _progress = MutableLiveData<Long>()
    val progress: LiveData<Long>
        get() = _progress

    private val _status = MutableLiveData<DownloadStatus>(DownloadStatus.NOT_STARTED)
    val status: LiveData<DownloadStatus>
        get() = _status

    var job: Job? = null
    private val newFileName = UUID.randomUUID().toString().take(6)


    private val pathExternal = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    fun downloadFile(toast: (message: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _status.postValue(DownloadStatus.IN_PROGRESS)
            val client = OkHttpClient();
            val request: Request
            try {
                request = Request.Builder().url(URL(urlString)).build();
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) { toast(ex.message!!) }
                return@launch
            }
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) { toast(response.message) }
            }

            if (response.body != null) {
                val contentType = response.header("content-type", null)
                var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                ext = if (ext == null) {
                    ""
                } else {
                    ".$ext"
                }
                val inputStream: InputStream = response.body!!.byteStream()
                val input = BufferedInputStream(inputStream)

                val file = File("${pathExternal}/${newFileName}.$ext")
                val output: OutputStream = FileOutputStream(file)

                job = launch() {
                    try {
                        val data = ByteArray(1024)

                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            output.write(data, 0, count)
                            _progress.postValue(total)
                            delay(1)
                        }
                        _progress.postValue(-1)
                        output.flush()
                        _status.postValue(DownloadStatus.COMPLETE)
                    }
                    catch (ex: CancellationException)
                    {
                        _status.postValue(DownloadStatus.CANCELLED)
                    }
                    catch (ex: Exception)
                    {
                        _status.postValue(DownloadStatus.FAILED)
                        file.delete()
                        _progress.postValue(0)
                        throw ex
                    } finally
                    {
                        output.close()
                        input.close()
                    }
                }
            }
        }
    }
}
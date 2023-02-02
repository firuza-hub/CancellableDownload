package com.example.cancellablefiledownload

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cancellablefiledownload.ui.DownloadStatus
import java.math.RoundingMode
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val df = DecimalFormat("#.###").apply { roundingMode = RoundingMode.DOWN }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnDownload = findViewById<Button>(R.id.btnDownload)
        val tvProgress = findViewById<TextView>(R.id.tvProgress)

        val etUrl = findViewById<EditText>(R.id.etUrl)

        btnDownload.setOnClickListener {
            if (etUrl.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter url", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.urlString = etUrl.text.toString()
            viewModel.downloadFile() {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
        btnCancel.setOnClickListener {
            if (viewModel.job != null) {
                viewModel.job!!.cancel()
            }
        }

        viewModel.progress.observe(this)
        {
            if (it > 0) {
                tvProgress.setTextColor(Color.GREEN)
                tvProgress.text = "${df.format(it / 1000000.0)}MB"
            }

        }

        viewModel.status.observe(this)
        {
            when (it) {
                DownloadStatus.NOT_STARTED -> {
                    tvProgress.setTextColor(Color.BLACK)
                    tvProgress.text = getString(R.string.click_download)
                }
                DownloadStatus.IN_PROGRESS -> {
                }
                DownloadStatus.COMPLETE -> {
                    tvProgress.setTextColor(Color.BLUE)
                    tvProgress.text = getString(R.string.msg_download_complete)
                }
                DownloadStatus.CANCELLED -> {
                    tvProgress.setTextColor(Color.RED)
                    tvProgress.text = getString(R.string.mesg_download_cancelled)
                }
                DownloadStatus.FAILED -> {
                    tvProgress.setTextColor(Color.RED)
                    tvProgress.text = getString(R.string.mesg_download_cancelled)
                }
            }

        }

    }
}
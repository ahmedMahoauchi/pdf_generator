package com.example.pdfgenerator

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {


    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PERMISSION_CAMERA = 101
    private val imageFiles = mutableListOf<File>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnTakePhoto: Button = findViewById(R.id.btnTakePhoto)
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_PERMISSION_CAMERA
                )
            } else {
                openCamera()
            }
        }

        val btnConvertToPdf: Button = findViewById(R.id.btnConvertToPdf)
        btnConvertToPdf.setOnClickListener {
            convertImagesToPdf(imageFiles)
        }

    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }


    private fun saveImage(image: Bitmap): File {
        val filename = "Image_" + UUID.randomUUID().toString() + ".jpeg"
        val file = File(getExternalFilesDir(null), filename)
        try {
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            return file
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show()
        }
        return file
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val imageFile = saveImage(imageBitmap)
            imageFiles.add(imageFile)
            Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun convertImagesToPdf(imageFiles: List<File>) {
        val outputPdfFile = File(getExternalFilesDir(null), "Output.pdf")
        val document = Document()

        try {
            val outputStream = FileOutputStream(outputPdfFile)
            PdfWriter.getInstance(document, outputStream)
            document.open()

            for (imageFile in imageFiles) {
                val image = Image.getInstance(imageFile.absolutePath)

                // Adjust image size to fit the page
                image.scaleToFit(document.pageSize)

                // Center the image on the page
                image.setAlignment(Image.MIDDLE)

                document.newPage()
                document.add(image)
            }

            document.close()
            Toast.makeText(this, "PDF created successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create PDF.", Toast.LENGTH_SHORT).show()
        }
        downloadPdf(outputPdfFile)

    }

    private fun downloadPdf(pdfFile: File) {
        if (pdfFile.exists()) {
            showFileNameInputDialog(this) { fileName ->
                if (fileName.isNotEmpty()) {
                    val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val destinationFile = File(downloadFolder, "$fileName.pdf")

                    try {
                        pdfFile.copyTo(destinationFile, overwrite = true)
                        Toast.makeText(this, "PDF downloaded successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to download the PDF.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "File name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "PDF file not found.", Toast.LENGTH_SHORT).show()
        }

    }

    fun showFileNameInputDialog(context: Context, onFileNameEntered: (String) -> Unit) {
        val editText = EditText(context)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Enter File Name")
            .setView(editText)
            .setPositiveButton("Download") { _, _ ->
                val fileName = editText.text.toString().trim()
                onFileNameEntered(fileName)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

}
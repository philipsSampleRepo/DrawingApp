package com.pay.drawingapp.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.pay.drawingapp.R
import com.pay.drawingapp.databinding.ActivityMainBinding
import com.pay.drawingapp.databinding.DialogBrushSizeBinding
import com.pay.drawingapp.databinding.ProgressDialogBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private lateinit var mainActivityBinding: ActivityMainBinding
private lateinit var dialogBrushSizeBinding: DialogBrushSizeBinding

private lateinit var dialogVeiw: View
private lateinit var progressDialog: Dialog
private lateinit var view: View

private lateinit var imageButton: ImageButton

class MainActivity : AppCompatActivity() {

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityResult(PHOTO_GALLERY, result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)
        view = mainActivityBinding.root
        setContentView(view)
        mainActivityBinding.drawingView.setBrushSize(5.toFloat())
        initDialog()
        imageButton = mainActivityBinding.palletLayout[7] as ImageButton
        imageButton.setImageDrawable(
            ContextCompat
                .getDrawable(this, R.drawable.pallete_pressed)
        )
        mainActivityBinding.drawingView.setBrushColor(imageButton.tag.toString())
        initGalleryBtn()
        undoFunctionality()
        saveFunctionality()
    }

    private fun initGalleryBtn() {

        mainActivityBinding.ibImagePicker.setOnClickListener {
            Log.i(TAG, "initGalaryBtn clicked...")
            if (isReadAccessGranted()) {
                pickPhotos()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun pickPhotos() {
        val pickPhotoIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(pickPhotoIntent)
    }

    private fun onActivityResult(requestCode: Int, result: ActivityResult) {

        if (result.resultCode == Activity.RESULT_OK) {
            if (requestCode == PHOTO_GALLERY) {

                result?.let {
                    it?.data?.let {
                        try {
                            mainActivityBinding.drawImageView.visibility = View.VISIBLE
                            mainActivityBinding.drawImageView.setImageURI(it.data)
                        } catch (e: Exception) {
                            Log.e(TAG, "onActivityResult: ", e.cause)
                        }
                    } ?: kotlin.run {
                        Toast.makeText(
                            this, "You dont have the image to be added...",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }


    private fun initDialog() {
        mainActivityBinding.ibBrush.setOnClickListener {
            showBrushSize()
        }
    }

    private fun showBrushSize() {
        val dialog = Dialog(this)
        dialogBrushSizeBinding = DialogBrushSizeBinding.inflate(layoutInflater)
        dialogVeiw = dialogBrushSizeBinding.root
        dialog.setContentView(dialogVeiw)
        dialog.setTitle("Set Brush Size")
        dialog.show()
        val small_button = dialogBrushSizeBinding.idSmallBtn
        val medium_button = dialogBrushSizeBinding.idMediumBtn
        val large_button = dialogBrushSizeBinding.idLargeBtn

        small_button.setOnClickListener {
            mainActivityBinding.drawingView.setBrushSize(5.toFloat())
            dialog.dismiss()
        }

        medium_button.setOnClickListener {
            mainActivityBinding.drawingView.setBrushSize(10.toFloat())
            dialog.dismiss()
        }

        large_button.setOnClickListener {
            mainActivityBinding.drawingView.setBrushSize(15.toFloat())
            dialog.dismiss()
        }
    }

    fun paintClicked(view: View) {
        if (view != imageButton) {
            val buttonImg = view as ImageButton
            val imageTag = buttonImg.tag.toString()
            mainActivityBinding.drawingView.setBrushColor(imageTag)
            buttonImg.setImageDrawable(
                ContextCompat
                    .getDrawable(this, R.drawable.pallete_pressed)
            )
            imageButton.setImageDrawable(
                ContextCompat
                    .getDrawable(this, R.drawable.pallete_normal)
            )
            imageButton = buttonImg
        }
    }


    fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(
                this, "You need to have the permission...",
                Toast.LENGTH_LONG
            ).show()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this, "You have the permission...",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this, "Permission denied...",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isReadAccessGranted(): Boolean {
        val access =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        return access == PackageManager.PERMISSION_GRANTED
    }

    private fun isWriteAccessGranted(): Boolean {
        val access =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        return access == PackageManager.PERMISSION_GRANTED
    }

    private fun undoFunctionality() {
        mainActivityBinding.ibUndo.setOnClickListener {
            Log.d(TAG, "undoFunctionality")
            mainActivityBinding.drawingView.undoDrawing()
        }
    }

    private fun saveFunctionality() {
        mainActivityBinding.ibSave.setOnClickListener {
            if (isWriteAccessGranted()) {
                CoroutineScope(Dispatchers.Main).launch {
                    saveImage(getBitmap(mainActivityBinding.imageFrame))
                }
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun initProgressDialog() {

        val progressDialogBinding: ProgressDialogBinding =
            ProgressDialogBinding.inflate(layoutInflater)
        val rootView = progressDialogBinding.root

        progressDialog = Dialog(this)
        progressDialog.setContentView(rootView)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private suspend fun saveImage(bitmap: Bitmap) {
        initProgressDialog()
        val out = CoroutineScope(Dispatchers.IO).async {
            var result = ""
            if (bitmap != null) {
                try {

                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val file =
                        File(
                            externalCacheDir?.absoluteFile.toString()
                                    + File.separator + "DrawingApp_"
                                    + System.currentTimeMillis() / 1000 + ".png"
                        )
                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(bytes.toByteArray())
                    result = file.absolutePath

                    if (fileOutputStream != null) {
                        fileOutputStream.close()
                    }
                } catch (e: Exception) {
                    result = ""
                    Log.e(TAG, "saveImage: ", e.cause)
                }
            }
            return@async result
        }
        val result = out.await()
        progressDialog.dismiss()
        if (result != null && !result.isEmpty()) {
            Toast.makeText(
                this, "File successfully saved... ${result}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this, "Failed to save the file...",
                Toast.LENGTH_LONG
            ).show()
        }

        MediaScannerConnection.scanFile(this, arrayOf(result), null) { s: String, uri: Uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private fun getBitmap(view: View): Bitmap {
        //bet the bitmap in the size of the view
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        //drew the bitmap to canvas
        val canvas = Canvas(bitmap)
        //get the background image to a drawable
        val bgBitmap: Drawable? = view.background

        bgBitmap?.let {
            //if the background drawable is not null then draw that to the canvas
            bgBitmap.draw(canvas)
        } ?: kotlin.run {
            // if it is null draw white to the canvas
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
    }

    companion object {
        private val TAG = MainActivity.javaClass.simpleName
        private const val STORAGE_PERMISSION_CODE = 10
        private const val PHOTO_GALLERY = 1
    }
}
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
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
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

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            processPermissions(permissions)
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
        readFunctionality()
        undoFunctionality()
        saveFunctionality()
    }

    private fun processPermissions(permissions: MutableMap<String, Boolean>) {
        permissions.entries.forEach {
            val permName = it.key
            val isGranted = it.value

            if (isGranted) {
                showSnackBar("You have the permission...${Manifest.permission.READ_EXTERNAL_STORAGE}")
                if (permName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    Log.i(TAG, "processPermissions: read")
                    pickPhotos()
                } else if (permName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    Log.i(TAG, "processPermissions: write")
                    savePhotos()
                }
            } else {
                if (permName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    showSnackBar("Permission denied...READ_EXTERNAL_STORAGE")
                } else if (permName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    showSnackBar("Permission denied...WRITE_EXTERNAL_STORAGE")
                }
            }
        }
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
                    } ?: run {
                        showSnackBar("You dont have the image to be added...")
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

    private fun requestWritePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            showSnackBar("You need to have the permission...")
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            )
        }
    }


    private fun requestReadPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ).toString()
            )
        ) {
            showSnackBar("You need to have the permission...")
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            )
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == STORAGE_PERMISSION_CODE) {
//            if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(
//                    this, "You have the permission...",
//                    Toast.LENGTH_LONG
//                ).show()
//            } else {
//                Toast.makeText(
//                    this, "Permission denied...",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }

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
                savePhotos()
            } else {
                requestWritePermission()
            }
        }
    }

    private fun savePhotos() {
        CoroutineScope(Dispatchers.Main).launch {
            saveImage(getBitmap(mainActivityBinding.imageFrame))
        }
    }

    private fun readFunctionality() {
        mainActivityBinding.ibImagePicker.setOnClickListener {
            Log.i(TAG, "initGalaryBtn clicked...")
            if (isReadAccessGranted()) {
                pickPhotos()
            } else {
                requestReadPermission()
            }
        }
    }

    private fun pickPhotos() {
        val pickPhotoIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(pickPhotoIntent)
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

    private fun saveImageWithReturn(bitmap: Bitmap): String {
        var result = ""
        if (bitmap != null) {
            try {
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                val dir =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        File(STORAGE_PATH)
                    } else {
                        File(STORAGE_PATH)
                    }
                if (!dir.exists()) {
                    dir.mkdir()
                }
                val file = File(
                    dir, "DrawingApp_"
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
                return result
                Log.e(TAG, "saveImage: ", e.cause)
            }
        }
        return result
    }

    private suspend fun saveImage(bitmap: Bitmap) {
        initProgressDialog()

        val result: String = withContext(Dispatchers.IO) {
            val out = lifecycleScope.async(Dispatchers.IO) {
                var result = ""
                if (bitmap != null) {
                    result = saveImageWithReturn(bitmap)
                }
                return@async result
            }
            return@withContext out.await()
        }

        withContext(Dispatchers.Main) {
            lifecycleScope.async(Dispatchers.Main) {
                progressDialog.dismiss()
                if (result != null && !result.isEmpty()) {
                    showSnackBar("File successfully saved... ${result}")
                } else {
                    showSnackBar("Failed to save the file...")
                }
                shareMedia(result)
            }
        }
    }

    private fun shareMedia(path: String) {
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(path),
            null
        ) { path, uri1 ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri1)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private fun getBitmap(view: View): Bitmap {
        //get the bitmap in the size of the view
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

    private fun showSnackBar(input: String) {
        Snackbar.make(
            this@MainActivity, mainActivityBinding.root,
            input, Snackbar.LENGTH_LONG
        )
            .show()
    }

    companion object {
        private val TAG = MainActivity.javaClass.simpleName
        private const val STORAGE_PERMISSION_CODE = 10
        private const val PHOTO_GALLERY = 1
        private const val STORAGE_PATH = "/sdcard/Documents/DrawingApp/"
    }

    /* private fun saveImageWithReturn1(bitmap: Bitmap): String {
         createDirectoryAndSaveFile()

 //        var dir: File
 //        if (Build.VERSION_CODES.R > Build.VERSION.SDK_INT) {
 //            dir = File(
 //                Environment.getDataDirectory().getPath()
 //                        + "//MyApp123"
 //            );
 //        } else {
 //            dir = File(
 //                Environment.getDataDirectory().getPath()
 //                        + "//MyApp123"
 //            );
 //        }
 //
 //        if (!dir.exists())
 //            dir.mkdir();
 //
 //
 //        val dir1 = File(Environment.getDataDirectory().path + "/" + "newDirName")
 //
 //        if (!dir1.exists()) {
 //            dir1.mkdir()
 //        }

 //        var data: File = Environment.getExternalStoragePublicDirectory
 //        (Environment.DIRECTORY_DOWNLOADS)
 //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
 //            data = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
 //        }
         ////

 //        val mydir = File(
 //            applicationContext.getExternalFilesDir("Directory Name")!!.absolutePath
 //        )
 //        if (!mydir.exists()) {
 //            mydir.mkdirs()
 //           // Toast.makeText(applicationContext, "Directory Created", Toast.LENGTH_LONG).show()
 //        }
 //
         var result = ""
         if (bitmap != null) {
             try {
                 val bytes = ByteArrayOutputStream()
                 bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
 //
 ////                var file: File;
 ////                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
 ////                    file = File(
 ////                        getExternalFilesDir(DIRECTORY_DOCUMENTS).toString() + "//DrawingApp_"
 ////                                + System.currentTimeMillis() / 1000 + ".png"
 ////                    )
 ////                } else {
 ////                    file = File(
 ////                        externalCacheDir, "//DrawingApp_"
 ////                                + System.currentTimeMillis() / 1000 + ".png"
 ////                    )
 ////                }
 //
 //                var file: File;
 //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
 //                    file = File(
 //                        getExternalFilesDir(DIRECTORY_DOCUMENTS).toString() + "//DrawingApp"
 //                    )
 //                } else {
 //                    file = File(
 //                        externalCacheDir, "//DrawingApp"
 //                    )
 //                }
 //
 //                if (!file.exists()) {
 //                    file.mkdir()
 //                }
 //
 //
 //                ///
 //                val file1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
 //                    File(
 //                        applicationContext.getExternalFilesDir(DIRECTORY_DOCUMENTS)!!.absolutePath+"//Test"
 //                    )
 //                } else {
 //                    TODO("VERSION.SDK_INT < KITKAT")
 //                }
 //
 //                if (!file1.exists()) {  //if directory by that name doesn't exist
 //                    if (!file1.mkdirs()) { //we create the directory
 //                        //if still directory doesn't get created ERROR mssg gets displayed
 //                        //which in most cases won't happen
 //                        Toast.makeText(this@MainActivity, "ERROR", Toast.LENGTH_SHORT).show()
 //                    }
 //                }
                 ////

                 val file =
                     File(Environment.getExternalStorageState() + "/Documents/DirName")
                 if (!file.exists()) {
                     val wallpaperDirectory = File("/sdcard/Documents/DirName/")
                     wallpaperDirectory.mkdirs()
                 }

                 val fileOutputStream = FileOutputStream(file)
                 fileOutputStream.write(bytes.toByteArray())
                 result = file.absolutePath

                 if (fileOutputStream != null) {
                     fileOutputStream.close()
                 }
             } catch (e: Exception) {
                 result = ""
                 return result
                 Log.e(TAG, "saveImage: ", e.cause)
             }
         }
         Log.i(TAG, "saveImageWithReturn: $result")
         return result
     }

     fun getAppSpecificAlbumStorageDir(context: Context, albumName: String): File? {
         // Get the pictures directory that's inside the app-specific directory on
         // external storage.
         val file = File(
             context.getExternalFilesDir(
                 Environment.DIRECTORY_PICTURES
             ), albumName
         )
         if (!file?.mkdirs()) {
             Log.e(TAG, "Directory not created")
         }
         return file
     }

     private fun saveTextFile(filename: String) {
         try {
             val saveTextFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
             saveTextFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
             saveTextFileIntent.type = "text/plain"
             saveTextFileIntent.putExtra(
                 Intent.EXTRA_TITLE,
                 "$filename.txt"
             )
             startActivityForResult(
                 saveTextFileIntent,
                 5
             )
         } catch (e: ActivityNotFoundException) {
             Log.e(TAG, "saveTextFile: ${e.cause}")
         }
     }


    private fun createDirectoryAndSaveFile() {
        val direct =
            File(Environment.getExternalStorageState() + "/Documents/DirName")
        if (!direct.exists()) {
            val wallpaperDirectory = File("/sdcard/Documents/DirName/")
            wallpaperDirectory.mkdirs()
        }
    }
     */
}
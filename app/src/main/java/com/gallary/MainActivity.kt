package com.gallary

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.gallary.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var pickUpGalleryImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var cameraImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var mCurrentPhotoPath: String? = null
    private var photoFile: File? = null
    private var photoUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register pick image activity result launcher
        pickUpGalleryImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Get the Uri of the selected image
                val data: Intent? = result.data
                val selectedImageUri: Uri? = data?.data
                selectedImageUri?.let {
                    binding.captureImage.setImageURI(it)
                }
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Register permission launcher for read external storage
        galleryPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pickUpGalleryImageLauncher.launch(pickUpPhotoFromGalleryIntent())
            } else {
                Toast.makeText(this, "Gallery Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

/*************************************************************************************************************************/
        // Register pick image activity result launcher
        cameraImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                photoFile?.let {
                    val fileUri: Uri = Uri.fromFile(it)
                    binding.captureImage.setImageURI(fileUri) // Display the image in ImageView
                }
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                cameraImageLauncher.launch(takeImageFromCameraIntent(photoUri = photoUri ?: "".toUri()))
            } else {
                Toast.makeText(this, "Camera Permission denied", Toast.LENGTH_SHORT).show()
            }
        }


        // Set up the gallery button click listener
        binding.gallaryBtn.setOnClickListener {
            checkAndRequestPermissionsForOpenGallery()
        }
        binding.cameraBtn.setOnClickListener {
            checkAndRequestPermissionsForOpenCamera()
        }
    }

    // Check if the necessary permissions are granted and request if not
    private fun checkAndRequestPermissionsForOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ use READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED
            ) {
                pickUpGalleryImageLauncher.launch(pickUpPhotoFromGalleryIntent())
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // For Android versions < 13 use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                pickUpGalleryImageLauncher.launch(pickUpPhotoFromGalleryIntent())
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Intent to pick an image from the gallery
    private fun pickUpPhotoFromGalleryIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
    }

    //**********************************************************************************************************************

    private fun checkAndRequestPermissionsForOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePicture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image: File = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }


    private fun takePicture() {
        try {
            photoFile = createImageFile()
            // Continue only if the File was successfully created
            photoFile?.let { file ->
                 photoUri = FileProvider.getUriForFile(
                    this,
                    "com.gallary.provider",
                    file
                )
                cameraImageLauncher.launch(takeImageFromCameraIntent(photoUri = photoUri ?: "".toUri()))

            }
        } catch (ex: Exception) {
            // Error occurred while creating the File
            displayMessage(baseContext, ex.message.toString())
        }

    }

    private fun takeImageFromCameraIntent(photoUri: Uri) = Intent().apply {
        action = MediaStore.ACTION_IMAGE_CAPTURE
        /**
         * The line [putExtra(MediaStore.EXTRA_OUTPUT, photoFile)] is used when you want to specify a file URI
         * where the camera app should save the captured image
         * [MediaStore.EXTRA_OUTPUT]: This is a key defined in the [MediaStore] class that's used to pass a URI to the camera app.
         * [photoFile]: This should be a Uri object representing the file where you want the captured image to be saved
         * */
        putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
    }

    private fun displayMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}


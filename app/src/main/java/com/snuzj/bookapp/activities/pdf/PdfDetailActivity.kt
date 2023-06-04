@file:Suppress("DEPRECATION")

package com.snuzj.bookapp.activities.pdf

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.snuzj.bookapp.R
import com.snuzj.bookapp.application.Constaints
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.ActivityPdfDetailBinding
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class PdfDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfDetailBinding
    private var bookId = ""
    private var bookTitle = ""
    private var bookUrl = ""
    private var isInMyFavorite = false

    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val TAG = "BOOK_DETAILS_TAG"
    }

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!
        MyApplication.icrementBookViewCount(bookId)

        loadBookDetails()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        binding.favoriteBtn.setOnClickListener{
            if(firebaseAuth.currentUser == null){
                //user not logged in, cant do favorite functionality
                Toast.makeText(this,"Bạn chưa đăng nhập",Toast.LENGTH_SHORT).show()
            }
            else{
                if(isInMyFavorite){
                    //already in fav, remove
                    MyApplication.removeFromFavorite(this,bookId)
                }
                else{
                    addToFavorite()
                }
            }
        }

        binding.downloadBookBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "OnCreate: STORAGE PERMISSION is granted")
                downloadBook()
            } else {
                Log.d(TAG, "OnCreate: STORAGE PERMISSION is denied")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        //init firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser != null){
            checkIsFavorite()
        }

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: Checking if book is in fav or not")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()
                    if(isInMyFavorite){
                        //available in favorite
                        Log.d(TAG, "onDataChange: available in favorite")
                        binding.favoriteBtn.setImageResource(R.drawable.baseline_favorite_24)
                    }
                    else{
                        Log.d(TAG, "onDataChange: not available in favorite")
                        binding.favoriteBtn.setImageResource(R.drawable.baseline_favorite_border_24)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "requestStoragePermissionLauncher: STORAGE PERMISSION is granted")
                downloadBook()
            } else {
                Log.d(TAG, "requestStoragePermissionLauncher: STORAGE PERMISSION is denied")
                Toast.makeText(this, "Từ chối truy cập", Toast.LENGTH_SHORT).show()
            }
        }



    private fun downloadBook() {
        Log.d(TAG, "downloadBook: Downloading book")
        progressDialog.setMessage("Đang tải sách")
        progressDialog.show()

        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constaints.MAX_BYTE_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "downloadBook: Book downloaded...")
                saveToDownLoadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "downloadBook: Download failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Tải xuống thất bại.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownLoadsFolder(bytes: ByteArray?) {
        Log.d(TAG, "saveToDownLoadsFolder: saving downloaded book")
        val nameWithExtension = "${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()

            val filePath = "${downloadsFolder.path}/$nameWithExtension"

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "Tải xuống thành công", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            incrementDownloadCount()
        } catch (e: Exception) {
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownLoadsFolder: failed to save due to ${e.message}")
            Toast.makeText(this, "Tải xuống thất bại vì ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        Log.d(TAG, "incrementDownloadCount: ")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var downloadsCount = "${snapshot.child("downloadsCount").value}"
                Log.d(TAG, "onDataChange: Current downloads count: $downloadsCount")

                if (downloadsCount == "" || downloadsCount == "null") {
                    downloadsCount = "0"
                }

                val newDownloadsCount: Long = downloadsCount.toLong() + 1
                Log.d(TAG, "onDataChange: New downloads count: $newDownloadsCount")

                val hashMap: HashMap<String, Any> = HashMap()
                hashMap["downloadsCount"] = newDownloadsCount

                val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                dbRef.child(bookId)
                    .updateChildren(hashMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "onDataChange: Downloads count incremented")
                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "onDataChange: Failed to incremented due to ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun loadBookDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryId = "${snapshot.child("categoryId").value}"
                val description = "${snapshot.child("description").value}"
                val downloadsCount = "${snapshot.child("downloadsCount").value}"
                val timestamp = "${snapshot.child("timestamp").value}"
                bookTitle = "${snapshot.child("title").value}"
                val uid = "${snapshot.child("uid").value}"
                bookUrl = "${snapshot.child("url").value}"
                val viewsCount = "${snapshot.child("viewsCount").value}"

                val date = MyApplication.formatTimeStamp(timestamp.toLong())

                MyApplication.loadCategory(categoryId, binding.categoryTv)

                MyApplication.loadPdfFromUrlSinglePage(
                    bookUrl,
                    bookTitle,
                    binding.pdfView,
                    binding.progressBar,
                    binding.pagesTv
                )

                MyApplication.loadPdfSize(bookUrl, bookTitle, binding.sizeTv)

                binding.titleTv.text = bookTitle
                binding.descriptionTv.text = description
                binding.viewsTv.text = viewsCount
                binding.downloadsTv.text = downloadsCount
                binding.dateTv.text = date
                binding.subTitleTv.text = bookTitle
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun addToFavorite(){
        Log.d(TAG, "addToFavorite: Adding to fav")
        val timestamp = System.currentTimeMillis()

        //setup data to add in db
        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp

        //save to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                //add to fav
                Log.d(TAG, "addToFavorite: Added to fav")
                Toast.makeText(this, "Thêm thành công",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                //failed to add to fav
                Log.d(TAG, "addToFavorite: Failed to add due to ${e.message}")
                Toast.makeText(this, "thêm vào thất bại",Toast.LENGTH_SHORT).show()

            }

    }

}

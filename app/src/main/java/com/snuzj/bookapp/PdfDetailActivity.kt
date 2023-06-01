@file:Suppress("DEPRECATION")

package com.snuzj.bookapp

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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.snuzj.bookapp.application.Constaints
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.ActivityPdfDetailBinding
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class PdfDetailActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding:ActivityPdfDetailBinding

    //book id get from intent
    private var bookId = ""

    //book title, url get from firebase
    private var bookTitle = ""
    private var bookUrl = ""
    private companion object{
        const val TAG = "BOOK_DETAILS_TAG"
    }

    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get book id from intent
        bookId = intent.getStringExtra("bookId")!!

        //increment book view count, whenviewer this page starts
        MyApplication.icrementBookViewCount(bookId)

        loadBookDetails()

        //handle button click , go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle button click, read more
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this,PdfViewActivity::class.java)
            intent.putExtra("bookId",bookId)
            startActivity(intent)
        }

        //handle click, download
        binding.downloadBookBtn.setOnClickListener {
            //first check permission
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "OnCreate: STORAGE PERMISSION is granted")
            }
            else{
                Log.d(TAG,"OnCreate: STORAGE PERMISSION is denied")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        //progressBar
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean->
        //Check if granted or denied
        if(isGranted){
            Log.d(TAG, "requestStoragePermissionLauncher: STORAGE PERMISSION is granted")
            downloadBook()
        }
        else{
            Log.d(TAG, "requestStoragePermissionLauncher: STORAGE PERMISSION is denied")
            Toast.makeText(this,"Từ chối truy cập",Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBook(){
        Log.d(TAG, "downloadBook: Downloading book")
        progressDialog.setMessage("Đang tải sách")
        progressDialog.show()

        //download from firebase
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constaints.MAX_BYTE_PDF)
            .addOnSuccessListener {bytes->
                Log.d(TAG, "downloadBook: Book downloaded...")
                saveToDownLoadsFolder(bytes)
            }
            .addOnFailureListener {e->
                Log.d(TAG, "downloadBook: Download failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Tải xuống thất bại.",Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveToDownLoadsFolder(bytes: ByteArray?) {
        Log.d(TAG, "saveToDownLoadsFolder: saving downloaded book")
        val nameWithExtention = "${System.currentTimeMillis()}.pdf"

        try{
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()

            val filePath = "${downloadsFolder.path}/$nameWithExtention"

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this,"Tải xuống thành công",Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            incrementDownloadCount()
        } catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownLoadsFolder: failed to save due to ${e.message}")
            Toast.makeText(this,"Tải xuống thất bại vì ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        //increment downloads count to firebase db
        Log.d(TAG, "incrementDownloadCount: ")

        // 1. Get previous downloads count
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get downloads count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Current downloads count: $downloadsCount")

                    if(downloadsCount == "" || downloadsCount =="null"){
                        downloadsCount = "0"
                    }

                    //convert to long and increment +1
                    val newDownloadsCount:Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: New downloads count: $newDownloadsCount")

                    //setup data to update to db
                    val hashMap: HashMap<String,Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadsCount

                    // 2. Update new incremented downloads count in db
                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Downloads count incremented")
                        }
                        .addOnFailureListener { e->
                            Log.d(TAG, "onDataChange: Failed to incremented due to ${e.message}")
                        }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

    private fun loadBookDetails() {
        //Books > bookId > Details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${ snapshot.child("timestamp").value }"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${ snapshot.child("uid").value }"
                    bookUrl = "${ snapshot.child("url").value }"
                    val viewsCount = "${ snapshot.child("viewsCount").value }"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    //load pdf thumbnail, pages count
                    MyApplication.loadPdfFromUrlSinglePage(bookUrl,
                        bookTitle, binding.pdfView, binding.progressBar, binding.pagesTv)

                    //load pdf size
                    MyApplication.loadPdfSize(bookUrl, bookTitle,binding.sizeTv)

                    //set data
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
}



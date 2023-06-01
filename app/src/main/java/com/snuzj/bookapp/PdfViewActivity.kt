package com.snuzj.bookapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.snuzj.bookapp.application.Constaints
import com.snuzj.bookapp.databinding.ActivityPdfViewBinding



@Suppress("DEPRECATION")
class PdfViewActivity : AppCompatActivity() {

    //viewBinding
    private lateinit var binding: ActivityPdfViewBinding

    //TAG
    private companion object{
        const val TAG = "PDF_VIEW_TAG"
    }

    //bookId
    private var bookId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!
        loadBookDetails()

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }


    }

    private fun loadBookDetails() {
        Log.d(TAG,"loadBookDetails: Get Pdf Url from db")
        //database reference to get book details, get book url using bookId
        // 1. Get Book Url
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book url, title
                    val pdfUrl = snapshot.child("url").value
                    val title = snapshot.child("title").value
                    Log.d(TAG, "onDataChange: PDF_URL: $pdfUrl")

                    //set toolbarTitleTv
                    binding.toolBarTitleTv.text = title.toString()

                    //2. Load pdf using url from firebase storage
                    loadBookFromUrl("$pdfUrl")
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


    }

    @SuppressLint("SetTextI18n")
    private fun loadBookFromUrl(pdfUrl: String) {
        Log.d(TAG, "loadBookFromUrl: Get Pdf from firebase storage using Url")

        val reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        reference.getBytes(Constaints.MAX_BYTE_PDF)
            .addOnSuccessListener {bytes->
                Log.d(TAG,"loadBookFromUrl: pdf got from url")

                //load pdf
                binding.pdfView.fromBytes(bytes)
                    .swipeHorizontal(true) //set false to scroll vertical, true for horizontal
                    .onPageChange{page, pageCount->
                        //set current and total pages in toolbar subtitle
                        val currentPage = page+1
                        binding.toolbarSubTitleTv.text = "$currentPage/$pageCount" // e.g: 1/74
                        Log.d(TAG,"loadBookFromUrl: $currentPage/$pageCount")
                    }
                    .onError{t->
                        Log.d(TAG,"loadBookFromUrl: ${t.message}")
                    }
                    .onPageError{_,t->
                        Log.d(TAG,"loadBookFromUrl: ${t.message}")
                    }
                    .load()
                binding.progressBar.visibility = View.GONE

            }
            .addOnFailureListener {e->
                Log.d(TAG,"loadBookFromUrl: Failed to get pdf due to ${e.message}")
                binding.progressBar.visibility = View.GONE
            }

    }
}
package com.snuzj.bookapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.ActivityPdfDetailBinding

@Suppress("DEPRECATION")
class PdfDetailActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding:ActivityPdfDetailBinding

    //book id
    private var bookId = ""

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

        // set the text of a TextView with the ID "subTitleTv" to match the TextView with the ID "titleTv"
        val subTitleTv = findViewById<TextView>(R.id.subTitleTv)
        val titleTv = findViewById<TextView>(R.id.titleTv)
        val text = titleTv.text.toString()
        subTitleTv.text = text

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
                    val title = "${snapshot.child("title").value}"
                    val uid = "${ snapshot.child("uid").value }"
                    val url = "${ snapshot.child("url").value }"
                    val viewsCount = "${ snapshot.child("viewsCount").value }"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    //load pdf thumbnail, pages count
                    MyApplication.loadPdfFromUrlSinglePage(url,
                        title, binding.pdfView, binding.progressBar, binding.pagesTv)

                    //load pdf size
                    MyApplication.loadPdfSize(url, title,binding.sizeTv)

                    //set data
                    binding.titleTv.text = title
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date
                    binding.subTitleTv.text = title

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}
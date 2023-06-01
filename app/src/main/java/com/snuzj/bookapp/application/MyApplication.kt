@file:Suppress("DEPRECATION")

package com.snuzj.bookapp.application

import android.annotation.SuppressLint
import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat




class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {

        //created a static method to convert timestamp to proper date format, so we can use it everywhere in project, no need to create again
        fun formatTimeStamp(timestamp: Long): String {
            val cal = Calendar.getInstance(Locale("vi", "VN"))
            cal.timeInMillis = timestamp
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
            return dateFormat.format(cal.time)
        }

        //function to get pdf size
        @SuppressLint("SetTextI18n")
        fun loadPdfSize(pdfUrl: String?, pdfTitle: String, sizeTv: TextView) {
            val TAG = "PDF_SIZE_TAG"

            //using url we can get file and its metadata from firebase storage
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl!!)
            ref.metadata
                .addOnSuccessListener { storageMetadata ->
                    Log.d(TAG, "loadPdfSize: got metadata")
                    val bytes = storageMetadata.sizeBytes.toDouble()
                    Log.d(TAG, "loadPdfSize: Size bytes $bytes")

                    //convert bytes to KB/MB
                    val kb = bytes / 1024
                    val mb = kb / 1024
                    if (mb >= 1) {
                        sizeTv.text = "${String.format("%.2f", kb)} MB"
                    } else if (kb >= 1) {
                        sizeTv.text = "${String.format("%.2f", kb)} KB"
                    } else {
                        sizeTv.text = "${String.format("%.2f", bytes)} bytes"
                    }

                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "loadPdfSize: Failed to get metadata due to ${e.message}")

                }
        }

            /*Instead of making new function loadPdfPageCount() to just load pages count if would be more good to use some existing function to do that
        * i.e. loadPdfFromUrlSinglePage
        * We will add another parameter of type TextView e.g. pagesTv
        * Whenever we call that function
        * 1. if we required page numbers we will pass pagesTv(TextView)
        * 2. if we dont require page number we will pass null
        * And in function if pagesTv(TextView) parameter is not null we will set the page number count*/
        fun loadPdfFromUrlSinglePage(
                pdfUrl: String?,
                pdfTitle: String,
                pdfView: PDFView,
                progressBar: ProgressBar,
                pagesTv: TextView?
        ){
                val TAG = "PDF_THUMBNAIL_TAG"

            //using url we can get file n its metadata from firebase storage
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl!!)
                ref.getBytes(Constaints.MAX_BYTE_PDF.toLong())
                    .addOnSuccessListener { bytes ->
                        Log.d(TAG, "loadPdfSize: Size bytes $bytes")

                        //set to pdfview
                        pdfView.fromBytes(bytes)
                            .pages(0)
                            .spacing(0)
                            .swipeHorizontal(false)
                            .enableSwipe(false)
                            .onError { t ->
                                progressBar.visibility = View.INVISIBLE
                                Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                            }
                            .onPageError { page, t ->
                                progressBar.visibility = View.INVISIBLE
                                Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                            }
                            .onLoad {nbPages ->
                                Log.d(TAG, "loadPdfFromUrlSinglePage: Pages: $nbPages")
                                //pdf loaded, we can set page count
                                progressBar.visibility = View.INVISIBLE

                                //if pagesTv param is not null then set pages number
                                if (pagesTv != null){
                                    pagesTv.text = "$nbPages"
                                }
                            }
                            .load()
                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "loadPdfSize: Failed to get metadata due to ${e.message}")

                    }
        }
        fun loadCategory(categoryId: String, categoryTv: TextView){
            //load category using category id from database
            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get category
                        val category = "${snapshot.child("category").value}"

                        //set category
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })


        }

        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTitle: String){
            /*param details
            * 1. context, used when require e.g. for progress dialog, toast
            * 2. bookId, to delete book from db
            * 3. bookUrl, delete book from firebase storage
            * 4. bookTitle, show in dialog
             */
            val TAG = "DELETE_BOOK_TAG"
            Log.d(TAG,"deleteBook: deleting...")

            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Đang tải")
            progressDialog.setMessage("Đang xóa: $bookTitle")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "deleteBook: Deleting from storage...")
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG,"delete book: Delete from storage")
                    Log.d(TAG,"delete book: Delete from db")
                    val ref = FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(context,"Xóa thành công.",Toast.LENGTH_SHORT).show()
                            Log.d(TAG,"delete book: Delete Successfully")
                        }

                }
                .addOnFailureListener {e->
                    Log.d(TAG,"delete book: Failed to delete due to ${e.message}")
                    progressDialog.dismiss()
                    Toast.makeText(context,"Xóa thất bại ${e.message}.",Toast.LENGTH_SHORT).show()
                }
        }

        fun icrementBookViewCount(bookId: String){
            //get current book views count
            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get views count
                        var viewsCount = "${ snapshot.child("viewsCount").value }"

                        if( viewsCount == "" || viewsCount == "null"){
                            viewsCount = "0"
                        }

                        val newViewsCount = viewsCount.toLong() + 1

                        //setup data to update in db
                        val hashMap = HashMap<String,Any>()
                        hashMap["viewsCount"] = newViewsCount

                        //set to db
                        val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                        dbRef.child(bookId)
                            .updateChildren(hashMap)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }
}


@file:Suppress("DEPRECATION")

package com.snuzj.bookapp.activities.pdf

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.snuzj.bookapp.models.ModelCategory
import com.snuzj.bookapp.databinding.ActivityPdfAddBinding

@Suppress("NAME_SHADOWING")
class PdfAddActivity : AppCompatActivity() {

    /*
        * setup view binding activity_pdf_add.xml
        * firebase auth
        * progress dialog
        * arraylist to hold pdf categories
        * uri of picked pdf
     */
    private lateinit var binding: ActivityPdfAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private var pdfUri: Uri? = null

    //tag
    private val TAG = "PDF_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        loadPdfCategories()

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, show category on pick
        binding.categoryTv.setOnClickListener {
            categoryPickDialog()
        }

        //handle click, pick pdf intent
        binding.attachPdfBtn.setOnClickListener {
            pdfPickIntent()
        }

        //handle click, start uploading pdf/book
        binding.submitBtn.setOnClickListener {
            /*
                * 1. Validate data
                * 2. Upload pdf to firebase storage
                * 3. Get url of uploaded pdf
                * 4. Upload pdf info to firebase db
             */

            validateData()
        }

    }

    private var title = ""
    private var description = ""
    private var category = ""

    private fun validateData() {
        //1. Validate data
        Log.d(TAG,"validateData: validating Data")

        //get data
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        category = binding.categoryTv.text.toString().trim()

        //validate data
        if(title.isEmpty()){
            Toast.makeText(this,"Hãy nhập tiêu đề cho sách",Toast.LENGTH_SHORT).show()
        }
        else if (description.isEmpty()){
            Toast.makeText(this,"Hãy nhập nội dung chính cho sách",Toast.LENGTH_SHORT).show()
        }
        else if (category.isEmpty()){
            Toast.makeText(this,"Hãy chọn thể loại",Toast.LENGTH_SHORT).show()
        }
        else if(pdfUri == null){
            Toast.makeText(this,"Hãy chọn file PDF",Toast.LENGTH_SHORT).show()
        }
        else{
            //data validated, begin upload
            uploadPdfToStorage()
        }

    }

    private fun uploadPdfToStorage() {
        //2. Upload pdf to firebase storage
        Log.d(TAG,"uploadPdfToStorage: uploading to storage...")

        //show progress dialog
        progressDialog.setMessage("Đang tải PDF lên CloudStorage.")
        progressDialog.show()

        //timestamp
        val timestamp = System.currentTimeMillis()

        //path of pdf in firebase storage
        val filePathAndName = "Books/$timestamp"

        //storage reference
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadPdfToStorage: PDF uploaded now getting url...")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                uriTask.addOnCompleteListener { uriTask ->
                    if (uriTask.isSuccessful) {
                        val uploadedPdfUrl = uriTask.result.toString()
                        uploadedPdfInfoToDb(uploadedPdfUrl, timestamp)
                    } else {
                            // Handling when URL cannot be obtained
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "uploadPdfToStorage: failed to upload due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Thao tác thất bại vì ${e.message}.", Toast.LENGTH_SHORT).show()
            }


    }

    private fun uploadedPdfInfoToDb(uploadedPdfUrl: String, timestamp: Long) {
        //4. Upload pdf info to firebase db
        Log.d(TAG, "uploadedPdfInfoToDb: uploading to db...")
        progressDialog.setMessage("Đang tải dữ liệu PDF.")

        //uid of current user
        val uid = firebaseAuth.uid

        //setup data to upload
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = title
        hashMap["description"] = description
        hashMap["categoryId"] = selectedCategoryId
        hashMap["url"] = uploadedPdfUrl
        hashMap["timestamp"] = timestamp
        hashMap["viewsCount"] = 0
        hashMap["downloadsCount"] = 0

        //db reference DB > Books > BookId > (BookInfo)
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG,"uploadPdfInfoToDb: uploaded to Db")
                progressDialog.dismiss()
                Toast.makeText(this,"Tải lên thành công.",Toast.LENGTH_SHORT).show()
                pdfUri = null
            }
            .addOnFailureListener { e->
                Log.d(TAG,"uploadPdfInfoToDb: failed to upload due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this,"Tải lên thất bại vì ${e.message}",Toast.LENGTH_SHORT).show()
            }

    }

    private fun loadPdfCategories() {
        Log.d(TAG,"Loading pdf categories.")
        //init arraylist
        categoryArrayList = ArrayList()

        //db reference to load categories PDF > Categories
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list be4 adding data
                categoryArrayList.clear()
                for (ds in snapshot.children){
                    //get data
                    val model = ds.getValue(ModelCategory::class.java)
                    //add to arraylist
                    categoryArrayList.add(model!!)
                    Log.d(TAG,"onDataChange: ${model.category}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""

    private fun categoryPickDialog(){
        Log.d(TAG, "categoryPickDialog: Showing pdf category pick dialog")

        //get string array of categories from arraylist
        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices){
            categoriesArray[i] = categoryArrayList[i].category
        }

        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn danh mục")
            .setItems(categoriesArray){dialog,which->
                //handle item click
                //get clicked item
                selectedCategoryTitle = categoryArrayList[which].category
                selectedCategoryId = categoryArrayList[which].id

                //set category to textview
                binding.categoryTv.text = selectedCategoryTitle

                Log.d(TAG,"categoryPickDialog: Selected Category ID: $selectedCategoryId")
                Log.d(TAG,"categoryPickDialog: Selected Category Title: $selectedCategoryTitle")

            }
            .show()
    }

    private fun pdfPickIntent(){
        Log.d(TAG, "pdfPickIntent: starting pdf pick intent")

        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfAcitivityResultLauncher.launch(intent)
    }

    val pdfAcitivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()

    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "PDF Picked: ")
            pdfUri = result.data!!.data
        } else {
            Log.d(TAG, "PDF Pick Cancelled")
            Toast.makeText(this, "Thu hồi", Toast.LENGTH_SHORT).show()
        }
    }
}
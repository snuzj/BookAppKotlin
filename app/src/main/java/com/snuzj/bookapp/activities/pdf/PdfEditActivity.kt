@file:Suppress("DEPRECATION")

package com.snuzj.bookapp.activities.pdf

import android.app.AlertDialog
import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.databinding.ActivityPdfEditBinding

class PdfEditActivity : AppCompatActivity() {

    //viewBinding
    private lateinit var binding: ActivityPdfEditBinding

    private companion object{
        private const val TAG = "PDF_EDIT_TAG"
    }


    //book id get from intent started from AdapterPdfAdmin
    private var bookId = ""

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    //arraylist to hold category titles
    private lateinit var categoryTitleArrayList: ArrayList<String>

    //arraylist to hold category ids
    private lateinit var categoryIdArrayList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get book id to edit book info
        bookId = intent.getStringExtra("bookId")!!

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        loadCategories()
        loadBookInfo()

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, pick category
        binding.categoryTv.setOnClickListener {
            categoryDialog()
        }
        //handle click, update book
        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private fun loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Loading book Info")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book info
                    selectCategoryId = snapshot.child("categoryId").value.toString()
                    val description = snapshot.child("description").value.toString()
                    val title = snapshot.child("title").value.toString()

                    //set to views
                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)

                    //load book category info using categoryId
                    Log.d(TAG, "onDataChange: Loading book category info")
                    val refBookCategory = FirebaseDatabase.getInstance().getReference("Categories")
                    refBookCategory.child(selectCategoryId)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                //get category
                                val category = snapshot.child("category").value
                                //set to textview
                                binding.categoryTv.text = category.toString()
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })


                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private var title = ""
    private var description = ""

    private fun validateData() {
        //get data
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()

        //validate data
        if(title.isEmpty()){
            Toast.makeText(this,"Hãy nhập tên cuốn sách.",Toast.LENGTH_SHORT).show()
        }
        else if(description.isEmpty()){
            Toast.makeText(this,"Hãy nhập nội dung của sách",Toast.LENGTH_SHORT).show()
        }
        else if(selectCategoryId.isEmpty()){
            Toast.makeText(this,"Hãy chọn thể loại cho sách",Toast.LENGTH_SHORT).show()
        }
        else{
            updatePdf()
        }
    }

    private fun updatePdf() {
        Log.d(TAG, "update Pdf: Starting updating pdf info...")

        //show progress
        progressDialog.setMessage("Đang cập nhật")
        progressDialog.show()

        //setup data to update to db
        val hashMap = HashMap<String, Any>()
        hashMap["title"] = title
        hashMap["description"] = description
        hashMap["categoryId"] = selectCategoryId

        //start updating
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                Log.d(TAG,"update pdf: Update successfully")
                progressDialog.dismiss()
                Toast.makeText(this,"Cập nhật thành công",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Log.d(TAG,"update pdf: Failed due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this,"Cập nhật thất bại ${e.message}",Toast.LENGTH_SHORT).show()
            }
    }

    private var selectCategoryId = ""
    private var selectCategoryTitle = ""

    private fun categoryDialog(){
        //show dialog to pick the category

        //make string array
        val categoriesArray = arrayOfNulls<String>(categoryTitleArrayList.size)
        for(i in categoryTitleArrayList.indices){
            categoriesArray[i] = categoryTitleArrayList[i]
        }

        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn danh mục")
            .setItems(categoriesArray){ dialog, position->
                //handle click, saved clicked category id n title
                selectCategoryId = categoryIdArrayList[position]
                selectCategoryTitle = categoryTitleArrayList[position]

                //set to TextView
                binding.categoryTv.text = selectCategoryTitle
            }
            .show() //show dialog

    }

    private fun loadCategories() {
        Log.d(TAG,"loadCategories: loading...")

        categoryTitleArrayList = ArrayList()
        categoryIdArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryIdArrayList.clear()
                categoryTitleArrayList.clear()

                for (ds in snapshot.children){

                    val id = "${ds.child("id").value}"
                    val category = "${ds.child("category").value}"

                    categoryIdArrayList.add(id)
                    categoryTitleArrayList.add(category)

                    Log.d(TAG,"onDataChange: categoryId $id")
                    Log.d(TAG,"onDataChange: categoryTitle $category")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}
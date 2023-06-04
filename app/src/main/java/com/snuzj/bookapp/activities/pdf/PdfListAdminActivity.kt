package com.snuzj.bookapp.activities.pdf

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.core.widget.addTextChangedListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.databinding.ActivityPdfListAdminBinding
import com.snuzj.bookapp.adapters.AdapterPdfAdmin
import com.snuzj.bookapp.models.ModelPdf

@Suppress("DEPRECATION")
class PdfListAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfListAdminBinding

    private var categoryId = ""
    private var category = ""

    //arraylist to hold books
    private lateinit var pdfArrayList:ArrayList<ModelPdf>
    //adapter
    private lateinit var adapterPdfAdmin: AdapterPdfAdmin

    private companion object{
        const val TAG = "PDF_LIST_ADMIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get from intent, that we can passed from adapter
        val intent = intent
        categoryId = intent.getStringExtra("categoryId")!!
        category = intent.getStringExtra("category")!!

        //set pdf category
        binding.subTitleTv.text = category

        //load pdf/books
        loadPdfList()

        //handle click, go back
        binding.backBtn.setOnClickListener{
            onBackPressed()
        }

        //search
        binding.searchEt.addTextChangedListener { object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {
                //filter data
                try{
                    adapterPdfAdmin.filter.filter(s)
                } catch (e:Exception){
                    Log.d(TAG,"OnTextChanged: ${e.message}")
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        }}

    }

    private fun loadPdfList() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list be4 start adding data into it
                    pdfArrayList.clear()
                    for (ds in snapshot.children){
                        //get data
                        val model = ds.getValue(ModelPdf::class.java)
                        //add to list
                        if (model != null) {
                            pdfArrayList.add(model)
                            Log.d(TAG, "OnDataChange: ${model.title} ${model.categoryId}")
                        }
                    }
                    val filterList = ArrayList<ModelPdf>()
                    adapterPdfAdmin = AdapterPdfAdmin(this@PdfListAdminActivity,pdfArrayList,filterList)
                    binding.booksRv.adapter = adapterPdfAdmin
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}
package com.snuzj.bookapp.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.databinding.FragmentBooksUserBinding
import com.snuzj.bookapp.models.ModelPdf
import com.snuzj.bookapp.adapters.AdapterPdfUser

class BooksUserFragment : Fragment() {

    private lateinit var binding: FragmentBooksUserBinding

    private var categoryId = ""
    private var category = ""
    private var uid = ""

    private lateinit var pdfArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfUser: AdapterPdfUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            categoryId = args.getString("categoryId")!!
            category = args.getString("category")!!
            uid = args.getString("uid")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBooksUserBinding.inflate(inflater, container, false)

        //loading pdf according to category, this fragment will have new instance to load each category pdfs
        Log.d(TAG, "onCreateView: Category $category")
        when (category) {
            "Tất cả" -> {
                //load all books
                loadAllBooks()
            }
            "Xem nhiều nhất" -> {
                //load most viewed books
                loadMostViewedDownloadedBooks("viewsCount")
            }
            "Tải về nhiều nhất" -> {
                //load most downloaded books
                loadMostViewedDownloadedBooks("downloadsCount")
            }
            else -> {
                loadCategorizedBooks() //load selected category book
            }
        }

        //search
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterPdfUser.filter.filter(s)
                } catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: SEARCH EXCEPTION ${e.message}")
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        return binding.root
    }

    private fun loadAllBooks() {
        //init list
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list before starting adding data into it
                pdfArrayList.clear()
                for (ds in snapshot.children) {
                    //get data
                    val model = ds.getValue(ModelPdf::class.java)
                    //add to list
                    pdfArrayList.add(model!!)
                }
                //setup adapter
                adapterPdfUser = AdapterPdfUser(context!!, pdfArrayList)
                //set adapter to recyclerview
                binding.booksRv.adapter = adapterPdfUser

            }

            override fun onCancelled(error: DatabaseError) {}

        })
    }

    private fun loadMostViewedDownloadedBooks(orderBy: String) {
        //init list
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild(orderBy).limitToLast(10) //ten books have most viewed
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before starting adding data into it
                    pdfArrayList.clear()
                    for (ds in snapshot.children) {
                        //get data
                        val model = ds.getValue(ModelPdf::class.java)
                        //add to list
                        pdfArrayList.add(model!!)
                    }
                    //setup adapter
                    adapterPdfUser = AdapterPdfUser(context!!, pdfArrayList)
                    //set adapter to recyclerview
                    binding.booksRv.adapter = adapterPdfUser

                }

                override fun onCancelled(error: DatabaseError) {}

            })
    }

    private fun loadCategorizedBooks() {
        //init list
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before starting adding data into it
                    pdfArrayList.clear()
                    for (ds in snapshot.children) {
                        //get data
                        val model = ds.getValue(ModelPdf::class.java)
                        //add to list
                        pdfArrayList.add(model!!)
                    }
                    //setup adapter
                    AdapterPdfUser(context!!, pdfArrayList).also { this@BooksUserFragment.adapterPdfUser = it }
                    //set adapter to recyclerview
                    binding.booksRv.adapter = adapterPdfUser

                }

                override fun onCancelled(error: DatabaseError) {}

            })
    }

    companion object {
        private const val TAG = "BOOKS_USER_TAG"

        //receive data from activity to load book
        fun newInstance(categoryId: String, category: String, uid: String): BooksUserFragment {
            val fragment = BooksUserFragment()
            //put data to bundle intent
            val args = Bundle()
            args.putString("categoryId", categoryId)
            args.putString("category", category)
            args.putString("uid", uid)
            fragment.arguments = args
            return fragment
        }
    }
}

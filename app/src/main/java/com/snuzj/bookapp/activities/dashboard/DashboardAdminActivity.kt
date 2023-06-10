package com.snuzj.bookapp.activities.dashboard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.activities.CategoryAddActivity
import com.snuzj.bookapp.activities.main.MainActivity
import com.snuzj.bookapp.activities.pdf.PdfAddActivity
import com.snuzj.bookapp.activities.profiles.ProfileActivity
import com.snuzj.bookapp.adapters.AdapterCategory
import com.snuzj.bookapp.models.ModelCategory
import com.snuzj.bookapp.databinding.ActivityDashboardAdminBinding
import java.lang.Exception

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardAdminBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private lateinit var adapterCategory: AdapterCategory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()
        loadCategories()

        //profile open
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        //search
        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterCategory.filter.filter(s)
                } catch (e: Exception){

                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        //handle click, log out
        binding.logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        //handle click, add category
        binding.addCategoryBtn.setOnClickListener {
            startActivity(Intent(this, CategoryAddActivity::class.java))
        }

        //handle click, add pdf
        binding.addPdfFab.setOnClickListener {
            startActivity(Intent(this, PdfAddActivity::class.java))
        }

    }

    private fun loadCategories() {
        //init arraylist
        categoryArrayList = ArrayList()

        //get all categories from db
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list  before starting adding data into it
                categoryArrayList.clear()
                for (ds in snapshot.children){
                    //get data as model
                    val model = ds.getValue(ModelCategory::class.java)

                    //add to arraylist
                    categoryArrayList.add(model!!)
                }
                //setup adapter
                adapterCategory = AdapterCategory(this@DashboardAdminActivity, categoryArrayList)
                //set adapter to recyclerview
                binding.categoriesRv.adapter = adapterCategory
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun checkUser() {
        //get current user
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser == null){
            //not logged in, go to main screen
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        else{
            //logged in, get n show user info
            val email = firebaseUser.email
            //set to textview of toolbar
            binding.subTitleTv.text = email
        }
    }
}
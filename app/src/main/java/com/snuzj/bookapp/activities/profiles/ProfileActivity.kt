@file:Suppress("DEPRECATION")

package com.snuzj.bookapp.activities.profiles

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.R
import com.snuzj.bookapp.adapters.AdapterPdfFavorite
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.ActivityProfileBinding
import com.snuzj.bookapp.models.ModelPdf

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var booksArrayList: ArrayList<ModelPdf>

    private lateinit var adapterPdfFavorite: AdapterPdfFavorite

    private lateinit var firebaseUser: FirebaseUser

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!
        loadUserInfo()
        loadFavoriteBooks()

        binding.accountTv.text = "N/A"
        binding.memberDateTv.text ="N/A"

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, open edit profile
        binding.profileEditBtn.setOnClickListener {
            startActivity(Intent(this,ProfileEditActivity::class.java))
        }
        //handle click, more Btn
        binding.moreBtn.setOnClickListener {
            moreOptionsDialog()
        }
    }

    private fun moreOptionsDialog() {
        //options to show in dialog
        val options = arrayOf("Xác minh tài khoản")
        val builder = AlertDialog.Builder(this)
        builder.setItems(options) { dialog, position ->
            //handle item click
            if (position == 0) {
                if (firebaseUser.isEmailVerified) {
                    //user verifired
                    Toast.makeText(this, "Tài khoản đã được xác minh", Toast.LENGTH_SHORT).show()
                } else {
                    emailVerificationDialog()
                }
            }
        }.show()
    }
    private fun emailVerificationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xác minh tài khoản")
            .setMessage("Bạn có chắc chắn gửi yêu cầu xác minh tài khoản tới ${firebaseUser.email}")
            .setPositiveButton("Gửi"){d,e->
                sendEmailVerification()
            }
            .setNegativeButton("Hủy bỏ"){d,e->
                d.dismiss()
            }
            .show()
    }

    private fun sendEmailVerification() {
        //show progress dialog
        progressDialog.setMessage("Gửi yêu cầu xác minh tới email: ${firebaseUser.email}")
        progressDialog.show()

        //send instructions
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                //successfully sent
                progressDialog.dismiss()
                Toast.makeText(this,"Yêu cầu đã được gửi, hãy kiểm tra email của bạn",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"Yêu cầu gửi thất bại vì ${e.message}",Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadUserInfo() {
        //check if user is verified or not, change affect after re-login when u already verified email
        if (firebaseUser.isEmailVerified){
            binding.accountStatusIv.setImageResource(R.drawable.baseline_check_circle_24)
        }
        else{
            binding.accountStatusIv.setImageResource(R.drawable.transparentlogo)
        }



        //db ref to load user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val email = snapshot.child("email").value.toString()
                    val name = snapshot.child("name").value.toString()
                    val profileImage = snapshot.child("profileImage").value.toString()
                    val timestamp = snapshot.child("timestamp").value.toString()
                    val uid = snapshot.child("uid").value.toString()
                    val userType = snapshot.child("userType").value.toString()

                    //convert timestamp to peroper date format
                    val formattedDate = MyApplication.formatTimeStamp(timestamp.toLong())
                    //set data
                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.memberDateTv.text = "Đã tham gia ngày $formattedDate"
                    binding.accountTv.text = userType
                    //set image
                    try{
                        Glide.with(this@ProfileActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.profilelogo)
                            .into(binding.profileIv)

                    } catch (e: Exception){

                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun loadFavoriteBooks(){
        //init array
        booksArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear arraylist, be4 starting adding data
                    booksArrayList.clear()
                    for (ds in snapshot.children){
                        //get only id of the books
                        val bookId = ds.child("bookId").value.toString()

                        //set to model
                        val modelPdf = ModelPdf()
                        modelPdf.id = bookId

                        //add model to list
                        booksArrayList.add(modelPdf)
                    }
                    //set number of favorites books
                    binding.FavoritesTv.text = SpannableStringBuilder().apply {
                        val boldSpan = StyleSpan(Typeface.BOLD) //display bold style
                        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.black)) //display black textColor
                        val text = "${booksArrayList.size} Đang theo dõi"

                        append(text)
                        setSpan(boldSpan, 0, booksArrayList.size.toString().length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                        setSpan(colorSpan, 0, booksArrayList.size.toString().length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    }
                    adapterPdfFavorite = AdapterPdfFavorite(this@ProfileActivity,booksArrayList) //set adapter
                    binding.favoriteRv.adapter = adapterPdfFavorite //set adapter to recyclerview
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}



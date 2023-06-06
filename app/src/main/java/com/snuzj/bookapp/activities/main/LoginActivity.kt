package com.snuzj.bookapp.activities.main

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.activities.dashboard.DashboardAdminActivity
import com.snuzj.bookapp.activities.dashboard.DashboardUserActivity
import com.snuzj.bookapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init
        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, not have account, go to register page
        binding.noAccountTv.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        //handle click, begin login
        binding.loginBtn.setOnClickListener {
            /*Step
            * 1. Input Data
            * 2. Validate Data
            * 3. Login - Firebase Auth
            * 4. Check user type - Firebase Auth
            * If user - Move to User Dashboard
            * If admin - Move to Admin Dashboard */
            validateData()
        }

        binding.forgotTv.setOnClickListener {
            startActivity(Intent(this,ForgotPasswordActivity::class.java))
        }

    }

    private var email = ""
    private var password = ""

    private fun validateData() {
        //1. Input Data
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()

        //2. Validate Data
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this,"Vui lòng nhập địa chỉ email hợp lệ.", Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()){
            Toast.makeText(this,"Vui lòng nhập mật khẩu.", Toast.LENGTH_SHORT).show()
        }
        else{
            loginUser()
        }

    }

    private fun loginUser() {
        //3. Login - Firebase Auth

        //show progress
        progressDialog.setMessage("Đang đăng nhập.")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                checkUser()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"Đăng nhập thất bại vì ${e.message}.",Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUser() {
        /* 4. Check user type - Firebase Auth
        * If user - Move to User Dashboard
        * If admin - Move to Admin Dashboard */
        progressDialog.setMessage("Kiểm tra thông tin người dùng.")

        val firebaseUser = firebaseAuth.currentUser!!

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener{

                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()

                    //get user type e.g. user or admin
                    val userType = snapshot.child("userType").value
                    if (userType == "user"){
                        startActivity(Intent(this@LoginActivity, DashboardUserActivity::class.java))
                        finish()
                    }
                    else if(userType == "admin"){
                        startActivity(Intent(this@LoginActivity, DashboardAdminActivity::class.java))
                        finish()
                    }
                }
            })
    }
}
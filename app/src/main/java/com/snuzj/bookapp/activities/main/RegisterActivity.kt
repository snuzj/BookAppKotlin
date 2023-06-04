package com.snuzj.bookapp.activities.main

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.snuzj.bookapp.activities.dashboard.DashboardUserActivity
import com.snuzj.bookapp.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle back button click
        binding.backBtn.setOnClickListener{
            onBackPressed()
        }

        //handle click, begin register
        binding.registerBtn.setOnClickListener {
            /*Step
            * 1. Input Data
            * 2. Validate Data
            * 3. Create Account - Firebase Auth
            * 4. Save User Info - Firebase Realtime Database */
            validateData()
        }
    }

    private var name = ""
    private var email = ""
    private var password = ""

    private fun validateData() {
        //1. Input Data
        name = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        val cPassword = binding.cPasswordEt.text.toString().trim()

        //2. Validate Data
        if(name.isEmpty()){
            //empty name
            Toast.makeText(this,"Vui lòng nhập tên.",Toast.LENGTH_SHORT).show()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email patern
            Toast.makeText(this,"Vui lòng nhập địa chỉ email hợp lệ.",Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()){
            //empty password
            Toast.makeText(this,"Vui lòng nhập mật khẩu.",Toast.LENGTH_SHORT).show()
        }
        else if(cPassword.isEmpty()){
            Toast.makeText(this,"Vui lòng xác nhận lại mật khẩu.",Toast.LENGTH_SHORT).show()
        }
        else if(cPassword != password){
            Toast.makeText(this,"Mật khẩu không khớp.",Toast.LENGTH_SHORT).show()
        }
        else{
            createUserAccount()
        }
    }

    private fun createUserAccount() {
        //3. Create Account - Firebase Auth
        //show progress
        progressDialog.setMessage("Đang tạo tài khoản.")
        progressDialog.show()

        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
            updateUserInfo()
        }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this,"Thao tác thất bại vì ${e.message}.",Toast.LENGTH_SHORT).show()
            }

    }

    private fun updateUserInfo() {
        //4. Save User Info - Firebase Realtime Database
        progressDialog.setMessage("Đang lưu thông tin người dùng.")

        //timestamp
        val timestamp = System.currentTimeMillis()

        //get current user uid, since user is registered n get it
        val uid = firebaseAuth.uid

        //setup data to add in db
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = name
        hashMap["profileImage"] = "" //is empty, can change in profile edit
        hashMap["userType"] = "user"
        hashMap["timestamp"] = timestamp

        //set data to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //user info saved, open Dashboard
                progressDialog.dismiss()
                Toast.makeText(this,"Tạo tài khoản thành công.",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardUserActivity::class.java))
                finish()
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Lưu thông tin không thành công vì ${e.message}.",Toast.LENGTH_SHORT).show()
            }

    }
}
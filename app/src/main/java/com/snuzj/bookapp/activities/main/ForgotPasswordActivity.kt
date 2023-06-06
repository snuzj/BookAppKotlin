package com.snuzj.bookapp.activities.main

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.snuzj.bookapp.R
import com.snuzj.bookapp.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private var email = ""
    private fun validateData() {
        //get data
        email = binding.emailEt.text.toString().trim()

        //validateData
        if (email.isEmpty()){
            Toast.makeText(this,"Hãy nhập email của bạn",Toast.LENGTH_SHORT).show()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this,"Email không tồn tại",Toast.LENGTH_SHORT).show()
        }
        else{
            recoverPassword()
        }
    }

    private fun recoverPassword() {
        //show progress
        progressDialog.setMessage("Gửi yêu cầu lập lại mật khẩu tới $email")
        progressDialog.show()

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                //sent
                progressDialog.dismiss()
                Toast.makeText(this,"Yêu cầu đã được gửi tới \n$email",Toast.LENGTH_SHORT).show()


            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Yêu cầu gửi thất bại ${e.message}",Toast.LENGTH_SHORT).show()
            }
    }
}
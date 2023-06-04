@file:Suppress("DEPRECATION")

package com.snuzj.bookapp.activities.profiles

import android.app.Activity

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.snuzj.bookapp.R
import com.snuzj.bookapp.databinding.ActivityProfileEditBinding

@Suppress("DEPRECATION")
class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    //image uri(which we will pick)
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Đang tải")
        progressDialog.setCanceledOnTouchOutside(false)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        loadUserInfo()

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }

        binding.updateBtn.setOnClickListener {
            validateData()
        }

    }

    private var name = ""

    private fun validateData() {
        //get data
        name = binding.nameEt.text.toString().trim()

        //validate data
        if (name.isEmpty()){
            Toast.makeText(this,"Hãy nhập tên",Toast.LENGTH_SHORT).show()
        }
        else{
            //name is entered

            if(imageUri == null){
                //need to update without image
                updateProfile("")
            }
            else{
                uploadImage()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.setMessage("Đang tải ảnh đại diện.")
        progressDialog.show()

        val filePathAndName = "ProfileImages/"+firebaseAuth.uid //image path and name, use uid replace previous

        //storage ref
        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot->
                //image uploaded, get url of uploaded image
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                updateProfile(uploadedImageUrl)
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Không thể cập nhật ảnh.",Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Đang cập nhật lại hồ sơ của bạn")

        //setup info to update to db
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = name
        if (imageUri != null){
            hashMap["profileImage"] = uploadedImageUrl
        }

        //update to db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                //profile updated
                progressDialog.dismiss()
                Toast.makeText(this,"Hồ sơ của bạn đã được cập nhật",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"Cập nhật thất bại ${e.message}",Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {
        //db ref to load user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val name = "${ snapshot.child("name").value }"
                    val profileImage = "${ snapshot.child("profileImage").value }"

                    //set data
                    binding.nameEt.setText(name)
                    //set image
                    try{
                        Glide.with(this@ProfileEditActivity)
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

    private fun showImageAttachMenu(){
        //show popup menu with options camera,gallery to pick image

        //setup popup menu
        val popupMenu = PopupMenu(this,binding.profileIv)
        popupMenu.menu.add(Menu.NONE,0,0,"Camera")
        popupMenu.menu.add(Menu.NONE,1,1,"Gallery")
        popupMenu.show()

        //handle pop up menu item click
        popupMenu.setOnMenuItemClickListener {item->
            //get id of clicked item
            val id = item.itemId
            if(id == 0){
                //camera clicked
                pickImageCamera()
            }
            else if(id == 1){
                pickImageGallery() //gallery clicked
            }
            true
        }
    }

    private fun pickImageCamera() {
        //intent to pick
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE,"Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp_description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGallery() {
        //intent to pick image from gallery
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)

    }

    //used to handle result of camera intent (new way in replacement of start activity for results)
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        { result ->
            //get uri
            if(result.resultCode == Activity.RESULT_OK){

                binding.profileIv.setImageURI(imageUri)
            }
            else{
                //cancelled
                Toast.makeText(this,"Huỷ bỏ",Toast.LENGTH_SHORT).show()
            }

        }
    )
    //used to handle gallery intent (new way in replacement of start activity for results)
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        { result ->
            //get uri
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data

                binding.profileIv.setImageURI(imageUri)
            }
            else{
                //cancelled
                Toast.makeText(this,"Huỷ bỏ",Toast.LENGTH_SHORT).show()
            }

        })


}
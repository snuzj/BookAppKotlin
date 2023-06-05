package com.snuzj.bookapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.R
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.RowCommentBinding
import com.snuzj.bookapp.models.ModelComment

class AdapterComment(
    val context: Context, //context
    val commentArrayList: ArrayList<ModelComment> //arraylist to hold comments
) : RecyclerView.Adapter<AdapterComment.HolderComment>() {

    private lateinit var binding: RowCommentBinding

    private var firebaseAuth: FirebaseAuth

    init {
        firebaseAuth = FirebaseAuth.getInstance()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderComment {
        binding = RowCommentBinding.inflate(LayoutInflater.from(context),parent,false)
        return HolderComment(binding.root)
    }

    override fun getItemCount(): Int {
        return commentArrayList.size //get size of arraylist
    }

    override fun onBindViewHolder(holder: HolderComment, position: Int) {
        //get data, set data, handle click

        //get data
        val model = commentArrayList[position]
        val id = model.id
        val bookId = model.bookId
        val comment = model.comment
        val uid = model.uid
        val timestamp = model.timestamp

        //format date
        val date = MyApplication.formatTimeStamp(timestamp.toLong())

        //set data
        holder.dateTv.text = date
        holder.commentTv.text = comment

        //we dont have user name , profile picture but have uid, loading using that uid
        loadUserDetails(model, holder)

        //handle click, show dialog to delete comment
        holder.itemView.setOnClickListener {
            /*Requirements to delete comment
            * 1. User must logged in
            * 2. uid in comment(to be deleted) must be same as uid of current user, just delete own comment */
            if(firebaseAuth.currentUser != null && firebaseAuth.uid == uid){
                deleteCommentDialog(model, holder)
            }
        }

    }

    private fun deleteCommentDialog(model: ModelComment, holder: HolderComment) {
        //alert dialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Xoá bình luận?")
            .setMessage("Điều này không thể hoàn tác và bạn chắc chắn muốn xóa nó")
            .setPositiveButton("Xoá"){d,e->
                val bookId = model.bookId
                val commentId = model.id

                //delete comment
                val ref = FirebaseDatabase.getInstance().getReference("Books")
                ref.child(bookId).child("Comments").child(commentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context,"Xoá thành công",Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {e->
                        //failed to deleted
                        Toast.makeText(context,"Xóa thất bại vì ${e.message}",Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Huỷ bỏ"){d,e->
                d.dismiss()

            }
            .show()
    }


    private fun loadUserDetails(model: ModelComment, holder: AdapterComment.HolderComment) {
        val uid = model.uid
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get name, get profile image
                    val name = snapshot.child("name").value.toString()
                    val profileImage = snapshot.child("profileImage").value.toString()

                    //set data
                    holder.nameTv.text = name
                    try {
                        Glide.with(context)
                            .load(profileImage)
                            .placeholder(R.drawable.profilelogo)
                            .into(holder.profileIv)
                    }
                    catch (e: Exception){
                        //in case of exception due to image is empty , set default
                        holder.profileIv.setImageResource(R.drawable.profilelogo)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    inner class HolderComment(itemView: View): RecyclerView.ViewHolder(itemView){
        //init ui views of row_comment.xml
        val profileIv: ImageView = binding.profileIv
        val nameTv = binding.nameTv
        val dateTv = binding.dateTv
        var commentTv = binding.commentTv
    }
}
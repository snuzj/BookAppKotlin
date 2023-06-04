package com.snuzj.bookapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.snuzj.bookapp.activities.pdf.PdfListAdminActivity
import com.snuzj.bookapp.filters.FilterCategory
import com.snuzj.bookapp.models.ModelCategory
import com.snuzj.bookapp.databinding.RowCategoryBinding

class AdapterCategory: RecyclerView.Adapter<AdapterCategory.HolderCategory>, Filterable {

    private val context: Context
    var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory>
    private var filter: FilterCategory? = null
    private lateinit var  binding: RowCategoryBinding

    //constructor
    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList = categoryArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflate bind row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderCategory(binding.root)
    }

    override fun getItemCount(): Int {
        return categoryArrayList.size
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        //Get data, Set Data, Handle Click, ...

        //get data
        val model = categoryArrayList[position]
        val id = model.id
        val category = model.category
        val timestamp = model.timestamp
        val uid = model.uid

        //set data
        holder.categoryTv.text = category

        //handle click, delete category
        holder.deleteBtn.setOnClickListener {
            //confirm be4 delete
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Xoá danh mục")
                .setMessage("Bạn có chắc chắn muốn xóa không?")
                .setPositiveButton("Đồng ý"){a,d->
                    Toast.makeText(context,"Đang xóa",Toast.LENGTH_SHORT).show()
                    deleteCategory(model, holder)
                }
                .setNegativeButton("Quay lại"){a,d->
                    a.dismiss()
                }
                .show()
        }

        //handle click, start pdf list admin activity, also pas pdf id, title
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfListAdminActivity::class.java)
            intent.putExtra("categoryId", id)
            intent.putExtra("category", category)
            context.startActivity(intent)
        }

    }

    private fun deleteCategory(model: ModelCategory, holder: HolderCategory) {
        //get id of category to delete
        val id = model.id
        //firebaseDB > Categories > categoryId
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {

                Toast.makeText(context,"Xóa thành công.",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Toast.makeText(context,"Thao tác thất bại vì ${e.message}.",Toast.LENGTH_SHORT).show()
            }
    }

    override fun getFilter(): Filter {
       if ( filter == null){
           filter = FilterCategory(filterList, this)
       }
        return filter as FilterCategory
    }

    //viewholder class to hold/init UI views for row_category.xml
    inner class HolderCategory(itemView:View):RecyclerView.ViewHolder(itemView){
        var categoryTv: TextView = binding.categoryTv
        var deleteBtn: ImageButton = binding.deteleBtn
    }




}
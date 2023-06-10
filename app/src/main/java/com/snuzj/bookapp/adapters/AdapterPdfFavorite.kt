package com.snuzj.bookapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.snuzj.bookapp.activities.pdf.PdfDetailActivity
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.RowPdfFavoriteBinding
import com.snuzj.bookapp.models.ModelPdf

class AdapterPdfFavorite//constructor
    (//context
    private val context: Context,
    private var booksArrayList: ArrayList<ModelPdf> //arraylist to hold books
) : RecyclerView.Adapter<AdapterPdfFavorite.HolderPdfFavorite>() {

    private lateinit var binding: RowPdfFavoriteBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfFavorite {
        binding = RowPdfFavoriteBinding.inflate(LayoutInflater.from(context),parent,false) //bind,inflate row_pdf_fav.xml
        return HolderPdfFavorite(binding.root)
    }

    override fun getItemCount(): Int {
        return booksArrayList.size //return size of list | number of list items in list
    }

    override fun onBindViewHolder(holder: HolderPdfFavorite, position: Int) {
        //get data, set data, handle clicks, etc

        //get data , from Users > uid > Favorites
        val model = booksArrayList[position]

        loadBookDetails(model, holder)

        //handle click,open pdf details page, pass book id to load details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId",model.id) //book id
            context.startActivity(intent)
        }

        //handle click, removeFromFav
        holder.removeFavBtn.setOnClickListener {
            MyApplication.removeFromFavorite(context,model.id)
        }
    }

    private fun loadBookDetails(model: ModelPdf, holder: AdapterPdfFavorite.HolderPdfFavorite) {
        val bookId = model.id

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book info
                    val categoryId = snapshot.child("categoryId").value.toString()
                    val description = snapshot.child("description").value.toString()
                    val downloadsCount = snapshot.child("downloadsCount").value.toString()
                    val timestamp = snapshot.child("timestamp").value.toString()
                    val title = snapshot.child("title").value.toString()
                    val uid = snapshot.child(" uid").value.toString()
                    val url = snapshot.child("url").value.toString()
                    val viewsCount = snapshot.child("viewsCount").value.toString()

                    //set data to model
                    model.isFavorite = true
                    model.title = title
                    model.description = description
                    model.categoryId = categoryId
                    model.timestamp = timestamp.toLong()
                    model.uid = uid
                    model.url = url
                    model.viewsCount = viewsCount.toLong()
                    model.downloadsCount = downloadsCount.toLong()

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    MyApplication.loadCategory(categoryId,holder.categoryTv)
                    MyApplication.loadPdfFromUrlSinglePage(url,
                        title,holder.pdfView,holder.progressBar,null)
                    MyApplication.loadPdfSize(url, title,holder.sizeTv)

                    holder.titleTv.text = title
                    holder.descriptionTv.text = description
                    holder.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    inner class HolderPdfFavorite(itemView: View) : RecyclerView.ViewHolder(itemView){
        //init UI View
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var removeFavBtn = binding.removeFavBtn
        var descriptionTv = binding.descriptionTv
        var categoryTv = binding.categoryTv
        var sizeTv = binding.sizeTv
        var dateTv = binding.dateTv
    }



}
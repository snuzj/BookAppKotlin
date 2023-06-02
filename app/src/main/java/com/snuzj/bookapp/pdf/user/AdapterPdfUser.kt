package com.snuzj.bookapp.pdf.user

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.snuzj.bookapp.PdfDetailActivity
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.RowPdfUserBinding
import com.snuzj.bookapp.pdf.ModelPdf

class AdapterPdfUser(
    private var context: Context,
    var pdfArrayList: ArrayList<ModelPdf>
) : RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser>(), Filterable {

    private lateinit var binding: RowPdfUserBinding
    private var filterList: ArrayList<ModelPdf> = ArrayList()

    init {
        filterList.addAll(pdfArrayList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfUser {
        //inflate / bind layout
        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfUser(binding.root)
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size //return list size / number of records
    }

    override fun onBindViewHolder(holder: HolderPdfUser, position: Int) {
        //get data, set data, handle click

        //get data
        val model = pdfArrayList[position]
        val bookId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val uid = model.uid
        val url = model.url
        val timestamp = model.timestamp

        //convert time
        val date = MyApplication.formatTimeStamp(timestamp)

        //set data
        holder.titleTv.text = title
        holder.description.text = description
        holder.dateTv.text = date

        MyApplication.loadPdfFromUrlSinglePage(
            url,
            title,
            holder.pdfView,
            holder.progressBar,
            null,
        ) //no need number of pages

        MyApplication.loadCategory(categoryId, holder.category)

        MyApplication.loadPdfSize(url, title, holder.sizeTv)

        //handle click, open pdf details page
        holder.itemView.setOnClickListener {
            //pass book id in intent, that will be used to get pdf info
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId", bookId)
            context.startActivity(intent)
        }

    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                //value to be searched should not be null or empty
                if (constraint != null && constraint.isNotEmpty()) {
                    //not null nor empty
                    //change to lower case to remove case sensitivity
                    val searchString = constraint.toString().toLowerCase()
                    val filteredList = ArrayList<ModelPdf>()
                    for (model in filterList) {
                        if (model.title.toLowerCase().contains(searchString)) {
                            //searched value matches with title, add to list
                            filteredList.add(model)
                        }
                    }
                    results.count = filteredList.size
                    results.values = filteredList
                } else {
                    //return original
                    results.count = filterList.size
                    results.values = filterList
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.let {
                    pdfArrayList = it.values as ArrayList<ModelPdf>
                    notifyDataSetChanged()
                }
            }
        }
    }

    /*View Holder class row_pdf_user.xml */
    inner class HolderPdfUser(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //init Ui Components of row_pdf_user
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var description = binding.descriptionTv
        var category = binding.categoryTv
        var sizeTv = binding.sizeTv
        var dateTv = binding.dateTv
    }
}

package com.snuzj.bookapp.pdf

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.snuzj.bookapp.PdfEditActivity
import com.snuzj.bookapp.application.MyApplication
import com.snuzj.bookapp.databinding.RowPdfAdminBinding

class AdapterPdfAdmin
    (//constructor
    private var context: Context, //context
    var pdfArrayList: ArrayList<ModelPdf>, private val filterList: ArrayList<ModelPdf> //arraylist to hold pdfs
) : RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin>(),Filterable{


    //filter object
    private var filter: FilterPdfAdmin? = null

    //viewBinding
    private lateinit var binding: RowPdfAdminBinding


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfAdmin {
        //bind/inflate layout row_pdf_admin.xml
        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfAdmin(binding.root)
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size
    }

    override fun onBindViewHolder(holder: HolderPdfAdmin, position: Int) {
        //get data
        val model = pdfArrayList[position]
        val pdfId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val pdfUrl = model.url
        val timestamp = model.timestamp

        //convert timestamp to dd/MM/yyyy format
        val formattedDate = MyApplication.formatTimeStamp(timestamp)

        //set data
        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = formattedDate

        //load further details

        //category id
        MyApplication.loadCategory(categoryId, holder.categoryTv)

        //No need pages number, so passed || load pdf thumbnail
        MyApplication.loadPdfFromUrlSinglePage(pdfUrl, title, holder.pdfView, holder.progressBar, null)

        //load pdf size
        MyApplication.loadPdfSize(pdfUrl, title, holder.sizeTv)

        //handle click, show dialog with option 1) EditBook 2) DeleteBook
        holder.moreBtn.setOnClickListener {
            moreOptionsDialog(model, holder)
        }
    }

    private fun moreOptionsDialog(model: ModelPdf, holder: AdapterPdfAdmin.HolderPdfAdmin) {
        //get id, url, title of book
        val bookId = model.id
        val bookUrl = model.url
        val bookTitle = model.title

        //options to show in dialog
        val options = arrayOf("Sửa","Xóa") //Edit n Delete

        //alrt dialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Lựa chọn của bạn")
            .setItems(options){dialog, position->
                //handle item click
                if(position == 0){
                    //edit clicked
                    val intent = Intent(context, PdfEditActivity::class.java)
                    intent.putExtra("bookId",bookId)
                    context.startActivity(intent)
                }
                else if(position == 1){
                    //delete clicked
                    //show confirmation dialog
                    val builder = androidx.appcompat.app.AlertDialog.Builder(context)
                    builder.setTitle("Xoá sách đã chọn")
                        .setMessage("Bạn có chắc chắn muốn xóa không?")
                        .setPositiveButton("Đồng ý"){a,d->
                            Toast.makeText(context,"Đang xóa", Toast.LENGTH_SHORT).show()
                            MyApplication.deleteBook(context,bookId, bookUrl, bookTitle)
                        }
                        .setNegativeButton("Quay lại"){a,d->
                            a.dismiss()
                        }
                        .show()

                }
            }
            .show()

    }

    override fun getFilter(): Filter {
        if (filter == null){
            filter = FilterPdfAdmin(filterList, null)
        }
        return filter as FilterPdfAdmin
    }

    /*View Holder class for row_pdf_admin.xml*/
    inner class HolderPdfAdmin(itemView: View) : RecyclerView.ViewHolder(itemView){
        //UI Views of row_pdf_admin.xml
        val pdfView = binding.pdfView
        val progressBar = binding.progressBar
        val titleTv = binding.titleTv
        val descriptionTv = binding.descriptionTv
        val categoryTv = binding.categoryTv
        val sizeTv = binding.sizeTv
        val dateTv = binding.dateTv
        val moreBtn = binding.moreBtn


    }
}
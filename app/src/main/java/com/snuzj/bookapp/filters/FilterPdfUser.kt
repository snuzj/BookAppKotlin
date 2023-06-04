package com.snuzj.bookapp.filters

import android.widget.Filter
import com.snuzj.bookapp.adapters.AdapterPdfUser
import com.snuzj.bookapp.models.ModelPdf

class FilterPdfUser(
    private var filterList: ArrayList<ModelPdf>,
    private var adapterPdfUser: AdapterPdfUser
) : Filter() {

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()

        //value to be searched should not be null or empty
        if (constraint != null && constraint.isNotEmpty()) {
            //not null nor empty
            //change to lower case to remove case sensitivity
            val searchString = constraint.toString().toLowerCase()
            val filteredModels = ArrayList<ModelPdf>()
            for (model in filterList) {
                if (model.title.toLowerCase().contains(searchString)) {
                    //searched value matches with title, add to list
                    filteredModels.add(model)
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            //return original
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        results?.let {
            adapterPdfUser.pdfArrayList = it.values as ArrayList<ModelPdf>
            adapterPdfUser.notifyDataSetChanged()
        }
    }
}

package com.snuzj.bookapp.category

import android.widget.Filter


class FilterCategory
    (
    private var filterlist: ArrayList<ModelCategory>, //arraylist in which wanna search
    private var adapterCategory: AdapterCategory //adapter in which filter needa be implemented
) : Filter() {

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        //value should not be null and non empty
        if(constraint != null && constraint.isNotEmpty()){

            //change to upper case , or lower case to avoid case sens
            constraint = constraint.toString().uppercase()
            val filteredModels: ArrayList<ModelCategory> = ArrayList()
            for( i in 0 until filterlist.size){
                //validate
                if (filterlist[i].category.uppercase().contains(constraint)){
                    //add to filtered list
                    filteredModels.add(filterlist[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            //search value is either null or empty
            results.count = filterlist.size
            results.values = filterlist
        }

        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        //apply filter changes
        adapterCategory.categoryArrayList = results.values as ArrayList<ModelCategory>

        //notify changes
        adapterCategory.notifyDataSetChanged()
    }
}
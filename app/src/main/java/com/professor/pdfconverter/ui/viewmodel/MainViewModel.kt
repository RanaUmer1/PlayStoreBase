package com.professor.pdfconverter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.professor.pdfconverter.data.repository.DataRepository
import com.professor.pdfconverter.model.DataModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**

Created by Umer Javed
Senior Android Developer
Created on 11/06/2025 7:37 pm
Email: umerr8019@gmail.com

 */


@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataRepository, @ApplicationContext private val context: Context
) : ViewModel() {


    private val _categoryList = MutableStateFlow<List<DataModel>>(emptyList())
    val categoryList: StateFlow<List<DataModel>> = _categoryList




   /* fun loadCategory() {
        viewModelScope.launch(Dispatchers.IO) {

            val categoryList = repository.loadData<DataModel>(
                context,
                RemoteConfigKeys.ANIMAL_CATEGORY_JSON,
                "json/animal_category.json",
                TypeTokenHelper.categoryListType
            ).toMutableList().apply {
                add(0, CategoryModel(id = "-1", name = "All", sequence = "0"))
            }
            _categoryList.value = categoryList
        }
    }*/




}

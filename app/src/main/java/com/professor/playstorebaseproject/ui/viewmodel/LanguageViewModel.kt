package com.professor.playstorebaseproject.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.model.LanguageListItem
import com.professor.playstorebaseproject.model.LanguageModel
import com.professor.playstorebaseproject.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<LanguageUiState>>(UIState.Loading)
    val uiState: StateFlow<UIState<LanguageUiState>> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<LanguageModel?>(null)
    val selectedLanguage: StateFlow<LanguageModel?> = _selectedLanguage.asStateFlow()

    private val _adState = MutableStateFlow(AdState.NOT_LOADED)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    init {
        loadLanguageData()
    }

    fun loadLanguageData() {
        _uiState.value = UIState.Loading
        viewModelScope.launch {
            try {
                val languages = getAvailableLanguages()
                val displayList = createDisplayList(languages)
                val uiState = LanguageUiState(
                    languages = languages,
                    displayList = displayList,
                    selectedLanguage = getDefaultLanguage(languages)
                )
                _uiState.value = UIState.Success(uiState)
                _selectedLanguage.value = uiState.selectedLanguage
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e)
            }
        }
    }

    fun selectLanguage(language: LanguageModel) {
        _selectedLanguage.value = language
    }

    fun saveSelectedLanguage() {
        val language = _selectedLanguage.value
        if (language != null) {
            val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
            if (language.id != savedLanguageId) {
                appPreferences.setInt(AppPreferences.LANGUAGE_ID, language.id)
                appPreferences.setString(AppPreferences.LANGUAGE_CODE, language.code)
            }
        }
    }

    fun updateAdState(state: AdState) {
        _adState.value = state
    }

    fun isLanguageSelected(): Boolean {
        return _selectedLanguage.value != null
    }

    fun shouldShowOnboarding(): Boolean {
        return !appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)
    }

    private fun getAvailableLanguages(): List<LanguageModel> {
        return listOf(
            LanguageModel(1, R.drawable.flag_arabic, "Arabic", "ar"),
            LanguageModel(2, R.drawable.flag_english, "English", "en"),
            LanguageModel(3, R.drawable.flag_spanish, "Spanish", "es"),
            LanguageModel(4, R.drawable.flag_indonesia, "Indonesian", "in"),
            LanguageModel(6, R.drawable.flag_persian, "Persian", "fa"),
            LanguageModel(7, R.drawable.flag_hindi, "Hindi", "hi"),
            LanguageModel(8, R.drawable.flag_russia, "Russian", "ru"),
            LanguageModel(9, R.drawable.flag_portuguese, "Portuguese", "pt"),
            LanguageModel(10, R.drawable.flag_bangla, "Bengali", "bn"),
            LanguageModel(11, R.drawable.flag_turkey, "Turkish", "tr")
        )
    }

    private fun getDefaultLanguage(languages: List<LanguageModel>): LanguageModel {
        val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
        val savedLanguage = languages.find { it.id == savedLanguageId }

        val deviceLangCode = Locale.getDefault().language
        return savedLanguage ?: languages.find { it.code == deviceLangCode }  
            ?: languages.find { it.code == "en" } ?: languages.first()
    }

    private fun createDisplayList(languages: List<LanguageModel>): List<LanguageListItem> {
        val defaultLanguage = getDefaultLanguage(languages)
        val displayList = mutableListOf<LanguageListItem>()

        defaultLanguage.let {
            displayList.add(LanguageListItem.Header("Default"))
            displayList.add(LanguageListItem.Language(it))
        }

        displayList.add(LanguageListItem.Header("All Languages"))
        languages.filterNot { it == defaultLanguage }
            .forEach { displayList.add(LanguageListItem.Language(it)) }

        return displayList
    }
}

data class LanguageUiState(
    val languages: List<LanguageModel>,
    val displayList: List<LanguageListItem>,
    val selectedLanguage: LanguageModel
)

enum class AdState {
    NOT_LOADED, LOADING, LOADED, SHOWING, FAILED, RETRYING
}
package com.jerboa.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jerboa.DEBOUNCE_DELAY
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.toApiState
import it.vercruysse.lemmyapi.datatypes.Search
import it.vercruysse.lemmyapi.datatypes.SearchResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel() : ViewModel() {

    var searchRes: ApiState<SearchResponse> by mutableStateOf(ApiState.Empty)
        private set

    private var fetchCommunitiesJob: Job? = null

    fun searchAll(form: Search) {
        fetchCommunitiesJob?.cancel()
        fetchCommunitiesJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            searchRes = ApiState.Loading
            searchRes = API.getInstance().search(form).toApiState()
        }
    }

    companion object {
        class Factory() : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = SearchViewModel() as T
        }
    }
}
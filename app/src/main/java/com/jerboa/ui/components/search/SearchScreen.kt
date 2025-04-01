package com.jerboa.ui.components.search

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jerboa.JerboaAppState
import com.jerboa.R
import com.jerboa.api.ApiState
import com.jerboa.feat.BlurNSFW
import com.jerboa.model.SearchViewModel
import com.jerboa.ui.components.common.ApiEmptyText
import com.jerboa.ui.components.common.ApiErrorText
import com.jerboa.ui.components.common.LoadingBar
import com.jerboa.ui.components.community.list.CommunityListHeader
import com.jerboa.ui.components.community.list.CommunityListings
import it.vercruysse.lemmyapi.datatypes.Search
import it.vercruysse.lemmyapi.dto.SearchType
import it.vercruysse.lemmyapi.dto.SortType
import kotlinx.coroutines.launch

enum class SearchTab(
    @StringRes val textId: Int,
) {
    Comments(R.string.post_screen_comments),
    Posts(R.string.person_profile_posts),
    Communities(R.string.communities),
    Users(R.string.users),
}

@Composable
fun SearchScreen(
    appState: JerboaAppState,
    blurNSFW: BlurNSFW,
    drawerState: DrawerState,
    showAvatar: Boolean,
    padding: PaddingValues? = null,
) {
    Log.d("jerboa", "got to search screen")

    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Companion.Factory())
    val pagerState = rememberPagerState { SearchTab.entries.size }

    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (search.isNotEmpty()) {
            searchViewModel.searchAll(
                form =
                    Search(
                        q = search,
                        type_ = SearchType.All,
                        sort = SortType.TopAll,
                    ),
            )
        }
    }

    val scope = rememberCoroutineScope()

    val baseModifier = if (padding == null) {
        Modifier
    } else {
        Modifier
            .padding(padding)
            .consumeWindowInsets(padding)
            .systemBarsPadding()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = baseModifier,
            topBar = {
                CommunityListHeader(
                    openDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    search = search,
                    onSearchChange = {
                        search = it
                        searchViewModel.searchAll(
                            form =
                                Search(
                                    q = search,
                                    type_ = SearchType.All,
                                    sort = SortType.TopAll,
                                ),
                        )
                    },
                )
            },
            content = { padding ->
                when (val searchRes = searchViewModel.searchRes) {
                    ApiState.Empty -> ApiEmptyText()
                    is ApiState.Failure -> ApiErrorText(searchRes.msg)
                    ApiState.Loading -> {
                        LoadingBar(padding)
                    }

                    is ApiState.Success -> {
                        Column {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                tabs = {
                                    SearchTab.entries.forEachIndexed { index, tab ->
                                        Tab(
                                            selected = pagerState.currentPage == index,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                            text = { Text(text = stringResource(id = tab.textId)) },
                                        )
                                    }
                                },
                            )
                            HorizontalPager(
                                state = pagerState,
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxSize().padding(padding),
                            ) { tabIndex ->
                                when (tabIndex) {
                                    SearchTab.Comments.ordinal -> {
                                        LazyColumn {
                                            items(searchRes.data.comments) { commentView ->
                                                Text(commentView.comment.content)
                                            }
                                        }
                                    }

                                    SearchTab.Communities.ordinal -> {
                                        CommunityListings(
                                            communities = searchRes.data.communities,
                                            onClickCommunity = { cs ->
                                                appState.toCommunity(id = cs.id)
                                            },
                                            modifier =
                                                Modifier
                                                    .padding(padding)
                                                    .imePadding(),
                                            blurNSFW = blurNSFW,
                                            showAvatar = showAvatar,
                                        )
                                    }

                                    SearchTab.Posts.ordinal -> {
                                        LazyColumn {
                                            items(searchRes.data.posts) { postView ->
                                                Text(postView.post.name)
                                            }
                                        }
                                    }

                                    SearchTab.Users.ordinal -> {
                                        LazyColumn {
                                            items(searchRes.data.users) { userView ->
                                                Text(userView.person.name)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> Log.w("jerboa", "Unknow api state: $searchRes")
                }

            },
        )
    }
}

@Preview
@Composable
private fun SearchScreenPreview() {

}
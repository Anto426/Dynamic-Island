package com.anto426.dynamicisland.ui.settings.pages.dev

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anto426.dynamicisland.R
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// ====================================================================================================
// --- 1. DATA & NETWORK LAYER ---
// ====================================================================================================

data class GitHubUser(
    val login: String,
    val name: String?,
    val avatar_url: String,
    val bio: String?,
    val followers: Int,
    val following: Int,
)

data class GitHubRepo(
    val name: String,
    val description: String?,
    val stargazers_count: Int,
    val language: String?,
)

interface GitHubApi {
    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getRepos(@Path("username") username: String): List<GitHubRepo>
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.github.com/"

    val api: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }
}

// ====================================================================================================
// --- 2. REPOSITORY LAYER ---
// ====================================================================================================

class DeveloperRepository(private val gitHubApi: GitHubApi = RetrofitInstance.api) {
    suspend fun getDeveloperInfo(username: String): Pair<GitHubUser, List<GitHubRepo>> {
        return coroutineScope {
            val profileDeferred = async { gitHubApi.getUser(username) }
            val reposDeferred = async { gitHubApi.getRepos(username) }

            try {
                val profile = profileDeferred.await()
                val repos = reposDeferred.await()
                Pair(profile, repos)
            } catch (e: Exception) {
                Log.e("DeveloperRepository", "Eccezione catturata nel Repository:", e)
                throw e
            }
        }
    }
}

// ====================================================================================================
// --- 3. VIEWMODEL LAYER ---
// ====================================================================================================

data class DeveloperInfo(
    val profile: GitHubUser,
    val repositories: List<GitHubRepo>
)

sealed class DeveloperUiState {
    object Loading : DeveloperUiState()
    data class Success(val data: DeveloperInfo) : DeveloperUiState()
    data class Error(val message: String) : DeveloperUiState()
}

class DeveloperViewModel(
    private val repository: DeveloperRepository = DeveloperRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeveloperUiState>(DeveloperUiState.Loading)
    val uiState: StateFlow<DeveloperUiState> = _uiState.asStateFlow()

    fun fetchDeveloperInfo(username: String) {
        viewModelScope.launch {
            _uiState.value = DeveloperUiState.Loading
            try {
                val (profile, repos) = repository.getDeveloperInfo(username)
                _uiState.value = DeveloperUiState.Success(DeveloperInfo(profile, repos))
            } catch (e: Exception) {
                Log.e("DeveloperViewModel", "Errore durante il fetch dei dati:", e)
                _uiState.value = DeveloperUiState.Error(e.message ?: "Errore di rete sconosciuto")
            }
        }
    }
}

// ====================================================================================================
// --- 4. UI (COMPOSE) LAYER ---
// ====================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    username: String = "Anto426",
    viewModel: DeveloperViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(username) {
        viewModel.fetchDeveloperInfo(username)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.developer_info_title)) }) }
    ) { padding ->
        DeveloperScreenContent(
            state = state,
            padding = padding,
            onRetry = { viewModel.fetchDeveloperInfo(username) }
        )
    }
}

@Composable
private fun DeveloperScreenContent(
    state: DeveloperUiState,
    padding: PaddingValues,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (state) {
            is DeveloperUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is DeveloperUiState.Success -> {
                DeveloperSuccessContent(state.data.profile, state.data.repositories)
            }
            is DeveloperUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun DeveloperSuccessContent(profile: GitHubUser, repos: List<GitHubRepo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DeveloperProfileCard(profile) }
        item {
            Text(
                text = stringResource(R.string.recent_repositories),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(repos) { repo -> RepositoryCard(repo) }
    }
}

@Composable
private fun DeveloperProfileCard(profile: GitHubUser) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = profile.avatar_url,
                contentDescription = stringResource(R.string.developer_avatar_desc),
                modifier = Modifier.size(96.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = profile.name ?: profile.login, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = profile.bio ?: stringResource(R.string.no_bio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(stringResource(R.string.followers_count, profile.followers))
                Text(stringResource(R.string.following_count, profile.following))
            }
        }
    }
}

@Composable
private fun RepositoryCard(repo: GitHubRepo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = repo.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = repo.description ?: stringResource(R.string.no_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.stars_count, repo.stargazers_count), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.language, repo.language ?: "N/D"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.error_message, message), color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry_button))
        }
    }
}
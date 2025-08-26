package com.anto426.dynamicisland.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.anto426.dynamicisland.R
import androidx.core.net.toUri

// --- DATA CLASSES ---
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
    val html_url: String
)

// --- API INTERFACE ---
interface GitHubApi {
    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getRepos(@Path("username") username: String): List<GitHubRepo>
}

// --- RETROFIT INSTANCE ---
object RetrofitInstance {
    private const val BASE_URL = "https://api.github.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GitHubApi::class.java)
    }
}

// --- REPOSITORY ---
class DeveloperRepository(private val gitHubApi: GitHubApi = RetrofitInstance.api) {
    suspend fun getDeveloperInfo(username: String): Pair<GitHubUser, List<GitHubRepo>> {
        return coroutineScope {
            val profileDeferred = async { gitHubApi.getUser(username) }
            val reposDeferred = async { gitHubApi.getRepos(username) }

            val profile = profileDeferred.await()
            val repos = reposDeferred.await()
            Pair(profile, repos)
        }
    }
}

// --- VIEWMODEL ---
data class DeveloperInfo(val profile: GitHubUser, val repositories: List<GitHubRepo>)

sealed class DeveloperUiState {
    object Loading : DeveloperUiState()
    data class Success(val data: DeveloperInfo) : DeveloperUiState()
    data class Error(val message: String) : DeveloperUiState()
}

class DeveloperViewModel(private val repository: DeveloperRepository = DeveloperRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow<DeveloperUiState>(DeveloperUiState.Loading)
    val uiState: StateFlow<DeveloperUiState> = _uiState.asStateFlow()

    fun fetchDeveloperInfo(username: String) {
        viewModelScope.launch {
            _uiState.value = DeveloperUiState.Loading
            try {
                val (profile, repos) = repository.getDeveloperInfo(username)
                _uiState.value = DeveloperUiState.Success(DeveloperInfo(profile, repos))
            } catch (e: Exception) {
                _uiState.value = DeveloperUiState.Error(e.message ?: "Errore di rete sconosciuto")
            }
        }
    }
}

// --- COMPOSABLES ---
@Composable
fun DeveloperScreen(username: String = "Anto426", viewModel: DeveloperViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(username) {
        viewModel.fetchDeveloperInfo(username)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            when (state) {
                is DeveloperUiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is DeveloperUiState.Error -> {
                    val message = (state as DeveloperUiState.Error).message
                    ErrorCard(message) { viewModel.fetchDeveloperInfo(username) }
                }

                is DeveloperUiState.Success -> {
                    val data = (state as DeveloperUiState.Success).data
                    DeveloperProfileCard(data.profile, context)
                    Spacer(Modifier.height(16.dp))
                    RepositoriesSection(data.repositories, context)
                }
            }
        }
    }
}

@Composable
fun DeveloperProfileCard(profile: GitHubUser, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = profile.avatar_url,
                contentDescription = "Avatar sviluppatore",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
            Text(profile.name ?: profile.login, style = MaterialTheme.typography.headlineSmall)
            Text(profile.login, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            InfoItemStyled(Icons.Default.Person, "Nome", profile.name ?: profile.login, context)
            InfoItemStyled(Icons.Default.Info, "Bio", profile.bio ?: R.string.no_description.toString(), context)
            InfoItemStyled(Icons.Default.Group, "Followers", profile.followers.toString(), context)
            InfoItemStyled(Icons.Default.PeopleOutline, "Following", profile.following.toString(), context)
        }
    }
}

@Composable
fun RepositoriesSection(repos: List<GitHubRepo>, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Repository recenti", style = MaterialTheme.typography.titleMedium)
        repos.forEach { repo ->
            RepositoryCard(repo, context)
        }
    }
}

@Composable
fun RepositoryCard(repo: GitHubRepo, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(repo.name, style = MaterialTheme.typography.titleMedium)
            Text(repo.description ?: "Nessuna descrizione", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = "Stelle", tint = MaterialTheme.colorScheme.primary)
                Text(
                    "${repo.stargazers_count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.LaptopChromebook, contentDescription = "Linguaggio", tint = MaterialTheme.colorScheme.secondary)
                Text(repo.language ?: "N/D", style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, repo.html_url.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Visualizza su GitHub")
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Riprova") }
        }
    }
}

// --- InfoItem Styled ---
@Composable
fun InfoItemStyled(
    icon: ImageVector,
    title: String,
    value: String,
    context: Context? = null,
    onClick: (() -> Unit)? = null
) {
    val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager?.setPrimaryClip(ClipData.newPlainText(title, value))
                Toast.makeText(context, "$title copiato!", Toast.LENGTH_SHORT).show()
                onClick?.invoke()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}



package com.anto426.dynamicisland.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // MODIFICATO: Import corretto
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource // NUOVO: Per usare le risorse stringa
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException

// --- DATA CLASSES, API, REPOSITORY (invariati) ---
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
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }).build())
            .build()
            .create(GitHubApi::class.java)
    }
}

class DeveloperRepository(private val gitHubApi: GitHubApi = RetrofitInstance.api) {
    suspend fun getDeveloperInfo(username: String): Pair<GitHubUser, List<GitHubRepo>> = coroutineScope {
        val profileDeferred = async { gitHubApi.getUser(username) }
        val reposDeferred = async { gitHubApi.getRepos(username) }
        Pair(profileDeferred.await(), reposDeferred.await())
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
            } catch (e: IOException) {
                _uiState.value = DeveloperUiState.Error("Controlla la tua connessione internet.")
            } catch (e: HttpException) {
                _uiState.value = DeveloperUiState.Error("Utente non trovato o errore del server.")
            } catch (e: Exception) {
                _uiState.value = DeveloperUiState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }
}

// --- COMPOSABLES ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class) // NUOVO
@Composable
fun DeveloperScreen(username: String = "Anto426", viewModel: DeveloperViewModel = viewModel(), onNavigateUp: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(username) {
        viewModel.fetchDeveloperInfo(username)
    }

    // NUOVO: Scaffold aggiunge una struttura standard all'app (es. TopAppBar)

        // NUOVO: AnimatedContent per transizioni fluide tra stati
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() with fadeOut() }, label = ""
        ) { targetState ->
            when (targetState) {
                is DeveloperUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is DeveloperUiState.Error -> ErrorCard(
                    message = targetState.message,
                    onRetry = { viewModel.fetchDeveloperInfo(username) }
                )

                is DeveloperUiState.Success -> DeveloperProfileContent(targetState.data)
            }
        }
}

// NUOVO: Composable per il contenuto della schermata di successo
@Composable
fun DeveloperProfileContent(data: DeveloperInfo) {
    // MODIFICATO: Uso corretto di LazyColumn
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DeveloperProfileCard(profile = data.profile)
        }

        item {
            ListHeader("Repository") // -> Da estrarre in strings.xml
        }

        // MODIFICATO: Uso di `items` per performance ottimali
        items(data.repositories) { repo ->
            RepositoryCard(repo = repo)
        }
    }
}

@Composable
fun DeveloperProfileCard(profile: GitHubUser) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = profile.avatar_url,
                contentDescription = "Avatar", // -> Da estrarre
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(16.dp))
            Text(profile.name ?: profile.login, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "@${profile.login}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            if (profile.bio != null) {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // MODIFICATO: Layout per followers/following più compatto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(icon = Icons.Default.Group, label = "Followers", value = profile.followers.toString())
                StatItem(icon = Icons.Default.PeopleOutline, label = "Following", value = profile.following.toString())
            }
        }
    }
}

@Composable
fun RepositoryCard(repo: GitHubRepo) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Rende l'intera card cliccabile
                val intent = Intent(Intent.ACTION_VIEW, repo.html_url.toUri())
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(repo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                repo.description ?: stringResource(id = R.string.no_description), // -> Esempio di uso di string resource
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RepoStat(icon = Icons.Default.Star, text = repo.stargazers_count.toString())
                if (repo.language != null) {
                    RepoStat(icon = Icons.Default.Code, text = repo.language)
                }
            }
        }
    }
}

// --- COMPONENTI UI RIUTILIZZABILI ---

// NUOVO: Componente per le statistiche (Followers/Following)
@Composable
fun StatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// NUOVO: Componente per le statistiche dei repo (stelle/linguaggio)
@Composable
fun RepoStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// NUOVO: Componente per i titoli delle sezioni
@Composable
fun ListHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Riprova") } // -> Da estrarre in strings.xml
    }
}
package com.anto426.dynamicisland.ui.settings.pages

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// --- DATA CLASSES, API, REPOSITORY (INVARIATO) ---
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

// --- VIEWMODEL (MODIFICATO) ---
data class DeveloperInfo(val profile: GitHubUser, val repositories: List<GitHubRepo>)

// FIX: Creiamo un tipo per l'errore che può contenere o un ID di risorsa o una stringa grezza
sealed class ErrorType {
    data class StringResource(@StringRes val id: Int) : ErrorType()
    data class RawString(val message: String) : ErrorType()
}

sealed class DeveloperUiState {
    object Loading : DeveloperUiState()
    data class Success(val data: DeveloperInfo) : DeveloperUiState()
    // FIX: La classe Error ora contiene un ErrorType, non più una String
    data class Error(val type: ErrorType) : DeveloperUiState()
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
                // FIX: Invia l'ID della risorsa, non la stringa
                _uiState.value = DeveloperUiState.Error(ErrorType.StringResource(R.string.dev_error_connection))
            } catch (e: HttpException) {
                // FIX: Invia l'ID della risorsa, non la stringa
                _uiState.value = DeveloperUiState.Error(ErrorType.StringResource(R.string.dev_error_not_found))
            } catch (e: Exception) {
                // FIX: Invia il messaggio dell'eccezione come stringa grezza o, se nullo, un ID di risorsa di fallback
                _uiState.value = DeveloperUiState.Error(
                    e.message?.let { ErrorType.RawString(it) }
                        ?: ErrorType.StringResource(R.string.dev_error_unknown)
                )
            }
        }
    }
}

// --- COMPONENTI UI RIUTILIZZABILI (MODIFICATO) ---

// Schermata di caricamento (invariato)
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                }
            }
            Text(text = stringResource(id = R.string.dev_loading_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(text = stringResource(id = R.string.dev_loading_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// Schermata di errore (MODIFICATA)
@Composable
fun ErrorScreen(errorType: ErrorType, onRetry: () -> Unit) {
    // FIX: La UI ora è responsabile di risolvere la risorsa stringa
    val message = when (errorType) {
        is ErrorType.StringResource -> stringResource(id = errorType.id)
        is ErrorType.RawString -> errorType.message
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(modifier = Modifier.size(96.dp), shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, shadowElevation = 4.dp) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Default.CloudOff, contentDescription = stringResource(id = R.string.error), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(text = stringResource(id = R.string.dev_error_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(id = R.string.retry))
            }
        }
    }
}


// --- COMPOSABLES (MODIFICATO) ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DeveloperScreen(username: String = "Anto426", viewModel: DeveloperViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(username) {
        viewModel.fetchDeveloperInfo(username)
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() with fadeOut() },
        modifier = Modifier.fillMaxSize(),
        label = "developer_screen_animation"
    ) { targetState ->
        when (targetState) {
            is DeveloperUiState.Loading -> LoadingScreen()
            is DeveloperUiState.Error -> ErrorScreen(
                // FIX: Passa l'oggetto ErrorType invece della stringa
                errorType = targetState.type,
                onRetry = { viewModel.fetchDeveloperInfo(username) }
            )
            is DeveloperUiState.Success -> DeveloperProfileContent(targetState.data)
        }
    }
}


// --- TUTTI GLI ALTRI COMPONENTI RIMANGONO INVARIATI ---
@Composable
fun DeveloperProfileContent(data: DeveloperInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.dev_profile_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = stringResource(id = R.string.dev_updated_from_github), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
        item { DeveloperProfileCard(profile = data.profile) }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text(text = stringResource(id = R.string.dev_public_repos_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "${data.repositories.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
        items(data.repositories) { repo -> RepositoryCard(repo = repo) }
        if (data.repositories.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(imageVector = Icons.Default.CodeOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = stringResource(id = R.string.dev_no_public_repos_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    Text(text = stringResource(id = R.string.dev_no_public_repos_desc), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun DeveloperProfileCard(profile: GitHubUser) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(modifier = Modifier.size(140.dp), shape = CircleShape, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), shadowElevation = 8.dp) {
                AsyncImage(model = profile.avatar_url, contentDescription = stringResource(id = R.string.avatar_of, profile.name ?: profile.login), modifier = Modifier.fillMaxSize().clip(CircleShape))
            }
            Text(text = profile.name ?: profile.login, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center, lineHeight = 36.sp)
            Text(text = "@${profile.login}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            if (profile.bio != null) {
                Text(text = profile.bio, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f), textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(icon = Icons.Default.People, label = "Followers", value = profile.followers.toString())
                StatItem(icon = Icons.Default.PersonAdd, label = "Following", value = profile.following.toString())
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { val url = "https://github.com/${profile.login}"; val intent = Intent(Intent.ACTION_VIEW, url.toUri()); context.startActivity(intent) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.open_profile))
                }
                OutlinedButton(onClick = { val clipboard = androidx.core.content.ContextCompat.getSystemService(context, android.content.ClipboardManager::class.java); clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("username", profile.login)); android.widget.Toast.makeText(context, "Username copiato", android.widget.Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.copy_username))
                }
            }
        }
    }
}

@Composable
fun RepositoryCard(repo: GitHubRepo) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().clickable { val intent = Intent(Intent.ACTION_VIEW, repo.html_url.toUri()); context.startActivity(intent) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(text = repo.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = stringResource(id = R.string.stars), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(text = repo.stargazers_count.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
            if (repo.description != null) {
                Text(text = repo.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp, maxLines = 3)
            } else {
                Text(text = stringResource(id = R.string.no_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (repo.language != null) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = getLanguageColor(repo.language)) {}
                            Text(text = repo.language, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(id = R.string.open_repository), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun getLanguageColor(language: String): androidx.compose.ui.graphics.Color {
    return when (language.lowercase()) {
        "kotlin" -> androidx.compose.ui.graphics.Color(0xFF7F52FF) // Purple
        "java" -> androidx.compose.ui.graphics.Color(0xFFED8B00) // Orange
        "javascript", "typescript" -> androidx.compose.ui.graphics.Color(0xFFF7DF1E) // Yellow
        "python" -> androidx.compose.ui.graphics.Color(0xFF3776AB) // Blue
        "c++", "c" -> androidx.compose.ui.graphics.Color(0xFF00599C) // Dark Blue
        "swift" -> androidx.compose.ui.graphics.Color(0xFFF05138) // Red-Orange
        "dart" -> androidx.compose.ui.graphics.Color(0xFF00B4AB) // Teal
        "go" -> androidx.compose.ui.graphics.Color(0xFF00ADD8) // Light Blue
        "rust" -> androidx.compose.ui.graphics.Color(0xFF000000) // Black
        "php" -> androidx.compose.ui.graphics.Color(0xFF777BB4) // Purple-Gray
        "ruby" -> androidx.compose.ui.graphics.Color(0xFFCC342D) // Red
        "html", "css" -> androidx.compose.ui.graphics.Color(0xFFE34F26) // Orange-Red
        else -> MaterialTheme.colorScheme.primary // Default
    }
}
package com.anto426.dynamicisland.ui.settings.pages

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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

// --- DATA CLASSES, API, REPOSITORY ---
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

// --- COMPONENTI UI RIUTILIZZABILI ---

// Schermata di caricamento moderna migliorata
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
            // Indicatore di progresso con sfondo
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }

            Text(
                text = "Caricamento informazioni sviluppatore...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Recupero dati da GitHub",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Schermata di errore moderna migliorata
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icona di errore con sfondo
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            shadowElevation = 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Errore",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Ops! Qualcosa è andato storto",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Riprova")
            }
        }
    }
}

// --- COMPOSABLES ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DeveloperScreen(username: String = "Anto426", viewModel: DeveloperViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(username) {
        viewModel.fetchDeveloperInfo(username)
    }

    // AnimatedContent per transizioni fluide tra stati
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() with fadeOut() },
        modifier = Modifier.fillMaxSize(),
        label = "developer_screen_animation"
    ) { targetState ->
        when (targetState) {
            is DeveloperUiState.Loading -> LoadingScreen()
            is DeveloperUiState.Error -> ErrorScreen(
                message = targetState.message,
                onRetry = { viewModel.fetchDeveloperInfo(username) }
            )
            is DeveloperUiState.Success -> DeveloperProfileContent(targetState.data)
        }
    }
}


// Contenuto della schermata di successo migliorato
@Composable
fun DeveloperProfileContent(data: DeveloperInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 16.dp,
            end = 24.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            DeveloperProfileCard(profile = data.profile)
        }

        item {
            // Header migliorato per i repository
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Repository Pubblici",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${data.repositories.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        items(data.repositories) { repo ->
            RepositoryCard(repo = repo)
        }

        // Messaggio se non ci sono repository
        if (data.repositories.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CodeOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Nessun repository pubblico",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Questo profilo non ha repository pubblici visibili",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DeveloperProfileCard(profile: GitHubUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Avatar con effetto glow
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                shadowElevation = 8.dp
            ) {
                AsyncImage(
                    model = profile.avatar_url,
                    contentDescription = "Avatar di ${profile.name ?: profile.login}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Nome e username con miglior spaziatura
            Text(
                text = profile.name ?: profile.login,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Text(
                text = "@${profile.login}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            // Bio con miglior leggibilità
            if (profile.bio != null) {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Statistiche con design migliorato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.People,
                    label = "Followers",
                    value = profile.followers.toString()
                )
                StatItem(
                    icon = Icons.Default.PersonAdd,
                    label = "Following",
                    value = profile.following.toString()
                )
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
                val intent = Intent(Intent.ACTION_VIEW, repo.html_url.toUri())
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con nome e stelle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Stelle con icona
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stelle",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = repo.stargazers_count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Descrizione con miglior gestione del testo
            if (repo.description != null) {
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            } else {
                Text(
                    text = "Nessuna descrizione disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Footer con linguaggio e link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Linguaggio
                if (repo.language != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = CircleShape,
                                color = getLanguageColor(repo.language)
                            ) {}
                            Text(
                                text = repo.language,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                // Icona link
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Apri repository",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Componenti di supporto migliorati
@Composable
fun StatItem(icon: ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ListHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

// Funzione per ottenere il colore del linguaggio di programmazione
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

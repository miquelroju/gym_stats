package com.myapp.gymstats.ui.features.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.data.remote.SupabaseClientProvider
import com.myapp.gymstats.domain.repository.WorkoutRepository
import com.myapp.gymstats.widget.WidgetEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState (
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String = "",
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabaseProvider: SupabaseClientProvider,
    private val repository: WorkoutRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val auth get() = supabaseProvider.client.auth

    init {
        checkCurrentSession()

        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val uid = status.session.user?.id ?: ""
                        _uiState.value = AuthUiState(
                            isAuthenticated = true,
                            userId = uid
                        )
                        WidgetEntryPoint.saveCurrentUserId(context, uid)

                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                            .addOnSuccessListener { token ->
                                viewModelScope.launch {
                                    repository.saveDeviceToken(uid, token)
                                }
                            }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _uiState.value = AuthUiState(isAuthenticated = false, userId = "")
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun checkCurrentSession() {
        viewModelScope.launch {
            val session = auth.currentSessionOrNull()
            if (session != null) {
                _uiState.value = AuthUiState(
                    isAuthenticated = true,
                    userId = session.user?.id ?: ""
                )
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                val userId = auth.currentUserOrNull()?.id ?: ""
                _uiState.value = AuthUiState(isAuthenticated = true, userId = userId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = mapAuthError(e)
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val userId = auth.currentUserOrNull()?.id ?: ""
                _uiState.value = AuthUiState(isAuthenticated = true, userId = userId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = mapAuthError(e)
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            _uiState.value = AuthUiState()
        }
    }

    private fun mapAuthError(e: Throwable): String {
        val message = e.message ?: ""
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Email o contraseña incorrectos"
            message.contains("User already registered", ignoreCase = true) ->
                "Ya existe una cuenta con este email"
            message.contains("Password should be at least", ignoreCase = true) ->
                "La contraseña debe tener al menos 6 caracteres"
            message.contains("Unable to validate email", ignoreCase = true) ->
                "El email no es válido"
            message.contains("network", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ->
                "Sin conexión. Comprueba tu internet"
            else -> "Ha ocurrido un error. Inténtalo de nuevo."
        }
    }
}

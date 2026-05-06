package com.myapp.gymstats.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.data.remote.SupabaseClientProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
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
    private val supabaseProvider: SupabaseClientProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val auth get() = supabaseProvider.client.auth

    init {
        checkCurrentSession()
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
                    error = e.message ?: "Error al registrarse"
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                auth.signInWith(Email) {
                    this.email
                    this.password
                }
                val userId = auth.currentUserOrNull()?.id ?: ""
                _uiState.value = AuthUiState(isAuthenticated = true, userId = userId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al inciar sesión"
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
}

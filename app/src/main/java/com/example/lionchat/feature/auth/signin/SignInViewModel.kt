package com.example.lionchat.feature.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lionchat.services.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel(){

    private val _state = MutableStateFlow<SignInState>(SignInState.Nothing)
    val state = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = SignInState.Loading
            val result = userRepository.signIn(email, password)
            _state.value = if (result.isSuccess) {
                SignInState.Success
            } else {
                SignInState.Error
            }
        }
    }
}

sealed class SignInState {
    data object Nothing : SignInState()
    data object Loading : SignInState()
    data object Success : SignInState()
    data object Error : SignInState()
}
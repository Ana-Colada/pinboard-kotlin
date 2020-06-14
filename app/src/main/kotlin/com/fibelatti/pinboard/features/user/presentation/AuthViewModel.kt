package com.fibelatti.pinboard.features.user.presentation

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import com.fibelatti.core.archcomponents.BaseViewModel
import com.fibelatti.core.archcomponents.LiveEvent
import com.fibelatti.core.archcomponents.MutableLiveEvent
import com.fibelatti.core.archcomponents.postEvent
import com.fibelatti.core.functional.onFailure
import com.fibelatti.core.provider.ResourceProvider
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.features.user.domain.Login
import com.fibelatti.pinboard.features.user.domain.LoginState
import com.fibelatti.pinboard.features.user.domain.UserRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.HttpURLConnection

class AuthViewModel @ViewModelInject constructor(
    private val loginUseCase: Login,
    private val userRepository: UserRepository,
    private val resourceProvider: ResourceProvider
) : BaseViewModel() {

    val loginState: LiveData<LoginState> get() = userRepository.getLoginState()

    val apiTokenError: LiveEvent<String> get() = _apiTokenError
    private val _apiTokenError = MutableLiveEvent<String>()

    fun login(apiToken: String) {
        launch {
            loginUseCase(apiToken)
                .onFailure { error ->
                    val loginFailedCodes = listOf(
                        HttpURLConnection.HTTP_UNAUTHORIZED,
                        HttpURLConnection.HTTP_INTERNAL_ERROR
                    )

                    if (error is HttpException && error.code() in loginFailedCodes) {
                        _apiTokenError.postEvent(resourceProvider.getString(R.string.auth_token_error))
                    } else {
                        handleError(error)
                    }
                }
        }
    }

    fun logout() {
        launch {
            userRepository.logout()
        }
    }
}

package com.noto.app.data.model.remote

sealed class ResponseException : RuntimeException() {

    operator fun invoke(): Nothing = throw this

    sealed class Auth : ResponseException() {
        object UserAlreadyRegistered : Auth()
        object InvalidEmail : Auth()
        object InvalidPassword : Auth()
        object InvalidLoginCredentials : Auth()
        object EmailNotVerified : Auth()
        object InvalidRefreshToken : Auth()
        class TooManyRequests(val seconds: Int) : Auth()
    }

    object NetworkErrorException : ResponseException()

}
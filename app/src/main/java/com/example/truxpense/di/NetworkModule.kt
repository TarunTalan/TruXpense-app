package com.example.truxpense.di

// Network dependencies

import android.content.Context
import com.example.truxpense.R
import com.example.truxpense.data.remote.authenticator.AuthAuthenticator
import com.example.truxpense.data.session.AuthSessionManager
import com.example.truxpense.data.session.TokenManager
import com.example.truxpense.data.session.TokenRefresher
import com.example.truxpense.data.local.datastore.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.OnboardingApi
import com.example.truxpense.data.remote.api.ProfileApi
import com.example.truxpense.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PLACEHOLDER = "REPLACE_WITH_BACKEND_BASE_URL"

    private fun normalizeBaseUrl(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == PLACEHOLDER) {
            throw IllegalStateException("Backend base URL is not configured. Set 'backendBaseUrl' in your local gradle properties or gradle-local.properties.")
        }

        var url = trimmed
        if (!url.contains("://")) {
            url = "http://$url"
        }
        if (!url.endsWith("/")) url += "/"
        return url
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Provides
    @Singleton
    fun provideTokenManager(prefs: AuthPreferences, appScope: CoroutineScope): TokenManager = TokenManager(prefs, appScope)

    @Provides
    @Singleton
    fun provideTokenRefresher(authApi: AuthApi, prefs: AuthPreferences): TokenRefresher = TokenRefresher(authApi, prefs)

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor = AuthInterceptor(tokenManager)

    @Provides
    @Singleton
    fun provideAuthAuthenticator(tokenRefresher: TokenRefresher, sessionManager: AuthSessionManager): AuthAuthenticator = AuthAuthenticator(tokenRefresher, sessionManager)

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @ApiRetrofit
    fun provideApiOkHttpClient(loggingInterceptor: HttpLoggingInterceptor, authInterceptor: AuthInterceptor, authAuthenticator: AuthAuthenticator): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)      // auth first — token added before logging
        .addInterceptor(loggingInterceptor)   // logging second — now sees Authorization header
        .authenticator(authAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(@ApplicationContext context: Context, @AuthRetrofit client: OkHttpClient): Retrofit {
        val raw = context.getString(R.string.backend_base_url)
        val baseUrl = normalizeBaseUrl(raw)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @ApiRetrofit
    fun provideApiRetrofit(@ApplicationContext context: Context, @ApiRetrofit client: OkHttpClient): Retrofit {
        val raw = context.getString(R.string.backend_base_url)
        val baseUrl = normalizeBaseUrl(raw)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@AuthRetrofit retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOnboardingApi(@ApiRetrofit retrofit: Retrofit): OnboardingApi = retrofit.create(OnboardingApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(@ApiRetrofit retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

}

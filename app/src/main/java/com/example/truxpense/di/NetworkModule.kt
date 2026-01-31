package com.example.truxpense.di

import android.content.Context
import com.example.truxpense.R
import com.example.truxpense.data.auth.AuthAuthenticator
import com.example.truxpense.data.auth.AuthSessionManager
import com.example.truxpense.data.auth.TokenManager
import com.example.truxpense.data.auth.TokenRefresher
import com.example.truxpense.data.prefs.AuthPreferences
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.remote.api.OnboardingApi
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

/**
 * Network module providing HTTP clients and Retrofit instances.
 *
 * This implementation reads `backend_base_url` from resources (populated at build time
 * from local gradle properties). It normalizes the value (adds scheme if omitted and
 * trailing slash) and does NOT hardcode any host in the source code.
 */
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

    // Normalize a provided base URL by trimming and ensuring a scheme and trailing slash.
    // This function does not hardcode any fallback; it uses the value supplied via local gradle properties.
    private fun normalizeBaseUrl(raw: String?): String {
        // Read the build-time resource value; require the developer to configure it locally.
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == PLACEHOLDER) {
            throw IllegalStateException("Backend base URL is not configured. Set 'backendBaseUrl' in your local gradle properties or gradle-local.properties.")
        }

        var url = trimmed
        // If the developer omitted the scheme, assume http by default.
        if (!url.contains("://")) {
            url = "http://$url"
        }
        // Ensure trailing slash
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
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
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

}

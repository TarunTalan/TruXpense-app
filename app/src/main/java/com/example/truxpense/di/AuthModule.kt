package com.example.truxpense.di

import android.content.Context
import com.example.truxpense.data.remote.api.AuthApi
import com.example.truxpense.data.repository.AuthRepository
import com.example.truxpense.data.repository.GoogleSignInRepository
import com.example.truxpense.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideGoogleSignInRepository(@ApplicationContext context: Context): GoogleSignInRepository =
        GoogleSignInRepository(context)

    @Provides
    @Singleton
    fun provideRetrofit(@ApplicationContext context: Context): Retrofit {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val baseUrl = context.getString(R.string.backend_base_url)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthRepository(api: AuthApi, prefs: com.example.truxpense.data.prefs.AuthPreferences): AuthRepository = AuthRepository(api, prefs)
}

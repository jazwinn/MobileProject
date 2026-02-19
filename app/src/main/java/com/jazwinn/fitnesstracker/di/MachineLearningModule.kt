package com.jazwinn.fitnesstracker.di

import android.content.Context
import com.jazwinn.fitnesstracker.data.ai.GeminiService
import com.jazwinn.fitnesstracker.data.repository.MachineGuideRepositoryImpl
import com.jazwinn.fitnesstracker.domain.ml.ImageClassifier
import com.jazwinn.fitnesstracker.domain.repository.MachineGuideRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MachineLearningModule {

    @Provides
    @Singleton
    fun provideImageClassifier(@ApplicationContext context: Context): ImageClassifier {
        return ImageClassifier(context)
    }

    @Provides
    @Singleton
    fun provideGeminiService(): GeminiService {
        return GeminiService()
    }

    @Provides
    @Singleton
    fun provideMachineGuideRepository(
        classifier: ImageClassifier,
        geminiService: GeminiService
    ): MachineGuideRepository {
        return MachineGuideRepositoryImpl(classifier, geminiService)
    }
}

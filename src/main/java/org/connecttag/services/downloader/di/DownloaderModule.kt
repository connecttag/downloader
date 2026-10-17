package org.connecttag.services.downloader.di

import org.connecttag.services.downloader.DownloadManagerImpl
import org.connecttag.services.downloader.IDownloadManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public abstract class DownloaderModule {

    @Binds
    @Singleton
    public abstract fun bindDownloadManager(impl: DownloadManagerImpl): IDownloadManager
}

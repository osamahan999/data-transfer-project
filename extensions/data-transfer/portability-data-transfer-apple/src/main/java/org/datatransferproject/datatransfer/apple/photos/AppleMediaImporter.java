/*
 * Copyright 2023 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.datatransfer.apple.photos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.jetbrains.annotations.NotNull;

/**
 * An Apple importer to import the Photos and Videos into Apple iCloud-photos.
 */
public class AppleMediaImporter implements Importer<TokensAndUrlAuthData, MediaContainerResource> {
  private final AppCredentials appCredentials;
  private final String exportingService;
  private final Monitor monitor;
  private final AppleInterfaceFactory factory;

  public AppleMediaImporter(
      @NotNull final AppCredentials appCredentials, @NotNull final Monitor monitor) {
    this(appCredentials, JobMetadata.getExportService(), monitor, new AppleInterfaceFactory());
  }

  @VisibleForTesting
  AppleMediaImporter(
    @NotNull final AppCredentials appCredentials, @NotNull  String exportingService,
    @NotNull final Monitor monitor, @NotNull  AppleInterfaceFactory factory) {
    this.appCredentials = appCredentials;
    this.exportingService = exportingService;
    this.monitor = monitor;
    this.factory = factory;
  }
  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      MediaContainerResource data)
      throws Exception {
    if (data == null) {
      return ImportResult.OK;
    }

    AppleMediaInterface mediaInterface = factory
      .getOrCreateMediaInterface(jobId, authData, appCredentials, exportingService, monitor);

    final int albumCount =
        mediaInterface.importAlbums(
            jobId,
            idempotentExecutor,
            data.getAlbums(),
          DataVertical.MEDIA.getDataType());
    final Map<String, Long> importPhotosMap =
        mediaInterface.importAllMedia(
            jobId,
            idempotentExecutor,
            data.getPhotos(),
          DataVertical.MEDIA.getDataType());
    final Map<String, Long> importVideosResult =
        mediaInterface.importAllMedia(
            jobId,
            idempotentExecutor,
            data.getVideos(),
          DataVertical.MEDIA.getDataType());

    final Map<String, Integer> counts =
        new ImmutableMap.Builder<String, Integer>()
            .put(MediaContainerResource.ALBUMS_COUNT_DATA_NAME, albumCount)
            .put(
                MediaContainerResource.PHOTOS_COUNT_DATA_NAME,
                importPhotosMap.getOrDefault(ApplePhotosConstants.COUNT_KEY, 0L).intValue())
            .put(
                MediaContainerResource.VIDEOS_COUNT_DATA_NAME,
                importVideosResult.getOrDefault(ApplePhotosConstants.COUNT_KEY, 0L).intValue())
            .build();

    return ImportResult.OK
        .copyWithBytes(
            importPhotosMap.getOrDefault(ApplePhotosConstants.BYTES_KEY, 0L)
                + importVideosResult.getOrDefault(ApplePhotosConstants.BYTES_KEY, 0L))
        .copyWithCounts(counts);
  }
}

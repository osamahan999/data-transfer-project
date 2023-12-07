package org.datatransferproject.datatransfer.google.common;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.cloud.google.GoogleJobStore;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

/**
 * Helper class that provides functionality for creating errors & logging them to data store.
 */
public class GoogleErrorLogger {

  public static void logFailedItemErrors(UUID jobId, ImmutableList<ErrorDetail> errorDetails, GoogleJobStore jobStore) throws IOException {
    jobStore.addErrorsToJob(jobId, errorDetails);
  }

  public static ErrorDetail createErrorDetail(String idempotentId, String itemName, Exception e, boolean canSkip) {
    return ErrorDetail.builder()
        .setId(idempotentId)
        .setTitle(itemName)
        .setException(Throwables.getStackTraceAsString(e))
        .setCanSkip(canSkip).build();
  }
}

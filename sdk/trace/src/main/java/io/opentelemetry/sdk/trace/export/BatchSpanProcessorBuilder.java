/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.trace.export;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.metrics.MeterProvider;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Builder class for {@link BatchSpanProcessor}. */
public final class BatchSpanProcessorBuilder {
  private static final Logger logger = Logger.getLogger(BatchSpanProcessorBuilder.class.getName());

  // Visible for testing
  static final long DEFAULT_SCHEDULE_DELAY_MILLIS = 5000;
  // Visible for testing
  static final int DEFAULT_MAX_QUEUE_SIZE = 2048;
  // Visible for testing
  static final int DEFAULT_MAX_EXPORT_BATCH_SIZE = 512;
  // Visible for testing
  static final int DEFAULT_EXPORT_TIMEOUT_MILLIS = 30_000;

  private final SpanExporter spanExporter;
  private boolean exportUnsampledSpans = false;
  private long scheduleDelayNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_SCHEDULE_DELAY_MILLIS);
  private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
  private int maxExportBatchSize = DEFAULT_MAX_EXPORT_BATCH_SIZE;
  private long exporterTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_EXPORT_TIMEOUT_MILLIS);
  private MeterProvider meterProvider = MeterProvider.noop();

  BatchSpanProcessorBuilder(SpanExporter spanExporter) {
    this.spanExporter = requireNonNull(spanExporter, "spanExporter");
  }

  /**
   * Sets whether unsampled spans should be exported. If unset, defaults to exporting only sampled
   * spans.
   *
   * @since 1.34.0
   */
  public BatchSpanProcessorBuilder setExportUnsampledSpans(boolean exportUnsampledSpans) {
    this.exportUnsampledSpans = exportUnsampledSpans;
    return this;
  }

  /**
   * Sets the delay interval between two consecutive exports. If unset, defaults to {@value
   * DEFAULT_SCHEDULE_DELAY_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setScheduleDelay(long delay, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(delay >= 0, "delay must be non-negative");
    scheduleDelayNanos = unit.toNanos(delay);
    return this;
  }

  /**
   * Sets the delay interval between two consecutive exports. If unset, defaults to {@value
   * DEFAULT_SCHEDULE_DELAY_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setScheduleDelay(Duration delay) {
    requireNonNull(delay, "delay");
    return setScheduleDelay(delay.toNanos(), TimeUnit.NANOSECONDS);
  }

  // Visible for testing
  long getScheduleDelayNanos() {
    return scheduleDelayNanos;
  }

  /**
   * Sets the maximum time an export will be allowed to run before being cancelled. If unset,
   * defaults to {@value DEFAULT_EXPORT_TIMEOUT_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setExporterTimeout(long timeout, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(timeout >= 0, "timeout must be non-negative");
    exporterTimeoutNanos = timeout == 0 ? Long.MAX_VALUE : unit.toNanos(timeout);
    return this;
  }

  /**
   * Sets the maximum time an export will be allowed to run before being cancelled. If unset,
   * defaults to {@value DEFAULT_EXPORT_TIMEOUT_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setExporterTimeout(Duration timeout) {
    requireNonNull(timeout, "timeout");
    return setExporterTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
  }

  // Visible for testing
  long getExporterTimeoutNanos() {
    return exporterTimeoutNanos;
  }

  /**
   * Sets the maximum number of Spans that are kept in the queue before start dropping. More memory
   * than this value may be allocated to optimize queue access.
   *
   * <p>Default value is {@code 2048}.
   *
   * @param maxQueueSize the maximum number of Spans that are kept in the queue before start
   *     dropping.
   * @return this.
   * @throws IllegalArgumentException if {@code maxQueueSize} is not positive.
   * @see BatchSpanProcessorBuilder#DEFAULT_MAX_QUEUE_SIZE
   */
  public BatchSpanProcessorBuilder setMaxQueueSize(int maxQueueSize) {
    checkArgument(maxQueueSize > 0, "maxQueueSize must be positive.");
    this.maxQueueSize = maxQueueSize;
    return this;
  }

  // Visible for testing
  int getMaxQueueSize() {
    return maxQueueSize;
  }

  /**
   * Sets the maximum batch size for every export. This must be smaller or equal to {@code
   * maxQueueSize}.
   *
   * <p>Default value is {@code 512}.
   *
   * @param maxExportBatchSize the maximum batch size for every export.
   * @return this.
   * @see BatchSpanProcessorBuilder#DEFAULT_MAX_EXPORT_BATCH_SIZE
   */
  public BatchSpanProcessorBuilder setMaxExportBatchSize(int maxExportBatchSize) {
    checkArgument(maxExportBatchSize > 0, "maxExportBatchSize must be positive.");
    this.maxExportBatchSize = maxExportBatchSize;
    return this;
  }

  /**
   * Sets the {@link MeterProvider} to use to collect metrics related to batch export. If not set,
   * metrics will not be collected.
   */
  public BatchSpanProcessorBuilder setMeterProvider(MeterProvider meterProvider) {
    requireNonNull(meterProvider, "meterProvider");
    this.meterProvider = meterProvider;
    return this;
  }

  // Visible for testing
  int getMaxExportBatchSize() {
    return maxExportBatchSize;
  }

  /**
   * Returns a new {@link BatchSpanProcessor} that batches, then converts spans to proto and
   * forwards them to the given {@code spanExporter}.
   *
   * @return a new {@link BatchSpanProcessor}.
   */
  public BatchSpanProcessor build() {
    if (maxExportBatchSize > maxQueueSize) {
      logger.log(
          Level.WARNING,
          "maxExportBatchSize should not exceed maxQueueSize. Setting maxExportBatchSize to {0} instead of {1}",
          new Object[] {maxQueueSize, maxExportBatchSize});
      maxExportBatchSize = maxQueueSize;
    }
    return new BatchSpanProcessor(
        spanExporter,
        exportUnsampledSpans,
        meterProvider,
        scheduleDelayNanos,
        maxQueueSize,
        maxExportBatchSize,
        exporterTimeoutNanos);
  }
}

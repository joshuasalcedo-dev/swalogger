{
  "metadata": {
    "exportTimestamp": "${exportTimestamp}",
    "frameworkVersion": "${frameworkVersion}",
    "exportFormat": "json",
    "generatedBy": "Joshua Salcedo Logging Framework - FreeMarker Template Engine"
  },
  "basicStatistics": {
    "totalLogs": ${stats.totalLogs},
    "droppedLogs": ${stats.droppedLogs},
    "droppedPercentage": ${stats.droppedPercentage},
    "handlerFailures": ${stats.handlerFailures},
    "errorRate": ${stats.errorRate},
    "throughputPerSecond": ${stats.throughputPerSecond},
    "configurationChanges": ${stats.configurationChanges}
  },
  "performance": {
    "avgProcessingTimeNanos": ${stats.avgProcessingTimeNanos},
    "avgProcessingTimeMicros": ${stats.avgProcessingTimeNanos / 1000},
    "slowestLogTimeNanos": ${stats.slowestLogTimeNanos},
    "slowestLogTimeMillis": ${stats.slowestLogTimeNanos / 1000000},
    "avgAsyncLatencyNanos": ${stats.avgAsyncLatencyNanos},
    "avgAsyncLatencyMicros": ${stats.avgAsyncLatencyNanos / 1000},
    "slowestAsyncLatencyNanos": ${stats.slowestAsyncLatencyNanos},
    "slowestAsyncLatencyMillis": ${stats.slowestAsyncLatencyNanos / 1000000}
  },
  "memory": {
    "currentUsageBytes": ${stats.currentMemoryUsage},
    "currentUsageMB": ${stats.currentMemoryUsage / 1024 / 1024},
    "peakUsageBytes": ${stats.peakMemoryUsage},
    "peakUsageMB": ${stats.peakMemoryUsage / 1024 / 1024}
  },
  "logLevelCounts": {
    <#list stats.logLevelCounts?keys as level>
    "${level}": ${stats.logLevelCounts[level]}<#if level_has_next>,</#if>
    </#list>
  },
  "loggerCounts": {
    <#list stats.loggerCounts?keys as logger>
    "${logger}": ${stats.loggerCounts[logger]}<#if logger_has_next>,</#if>
    </#list>
  },
  "handlerCounts": {
    <#list stats.handlerCounts?keys as handler>
    "${handler}": ${stats.handlerCounts[handler]}<#if handler_has_next>,</#if>
    </#list>
  },
  "errorCounts": {
    <#list stats.errorCounts?keys as error>
    "${error}": ${stats.errorCounts[error]}<#if error_has_next>,</#if>
    </#list>
  },
  "queueMetrics": {
    <#list stats.queueSizes?keys as queue>
    "${queue}": {
      "currentSize": ${stats.queueSizes[queue]},
      "maxSize": ${stats.maxQueueSizes[queue]!0},
      "avgSize": ${stats.avgQueueSizes[queue]!0}
    }<#if queue_has_next>,</#if>
    </#list>
  },
  "timeSeriesData": {
    "logRateHistory": [
      <#list stats.recentLogRates as rate>
      {
        "timestamp": "${rate.timestamp}",
        "logsPerSecond": ${rate.value}
      }<#if rate_has_next>,</#if>
      </#list>
    ],
    "errorRateHistory": [
      <#list stats.recentErrorRates as rate>
      {
        "timestamp": "${rate.timestamp}",
        "errorPercentage": ${rate.value}
      }<#if rate_has_next>,</#if>
      </#list>
    ]
  },
  "configuration": {
    "metricsEnabled": ${config.enabled?c},
    "autoReportEnabled": ${config.autoReportEnabled?c},
    "autoReportInterval": "${config.autoReportIntervalMinutes} minutes",
    "exportEnabled": ${config.exportEnabled?c},
    "exportFormat": "${config.exportFormat}",
    "tracking": {
      "memoryTracking": ${config.memoryTrackingEnabled?c},
      "performanceTracking": ${config.performanceTrackingEnabled?c},
      "errorTracking": ${config.errorTrackingEnabled?c},
      "queueTracking": ${config.queueTrackingEnabled?c},
      "methodTracking": ${config.methodTrackingEnabled?c}
    }
  },
  "systemInfo": {
    "javaVersion": "${systemInfo.javaVersion}",
    "javaVendor": "${systemInfo.javaVendor}",
    "osName": "${systemInfo.osName}",
    "osVersion": "${systemInfo.osVersion}",
    "availableProcessors": ${systemInfo.availableProcessors},
    "maxMemory": ${systemInfo.maxMemory},
    "totalMemory": ${systemInfo.totalMemory},
    "freeMemory": ${systemInfo.freeMemory}
  }
}
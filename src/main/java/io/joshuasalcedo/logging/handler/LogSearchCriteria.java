package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.LogLevel;

import java.time.Instant;

public class LogSearchCriteria {
    private final LogLevel minLevel;
    private final String loggerName;
    private final String className;
    private final Instant fromTime;
    private final Instant toTime;
    private final String messageKeyword;
    private final Boolean hasThrowables;
    private final int limit;

    private LogSearchCriteria(Builder builder) {
        this.minLevel = builder.minLevel;
        this.loggerName = builder.loggerName;
        this.className = builder.className;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime;
        this.messageKeyword = builder.messageKeyword;
        this.hasThrowables = builder.hasThrowables;
        this.limit = builder.limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LogLevel getMinLevel() {
        return minLevel;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getClassName() {
        return className;
    }

    public Instant getFromTime() {
        return fromTime;
    }

    public Instant getToTime() {
        return toTime;
    }

    public String getMessageKeyword() {
        return messageKeyword;
    }

    public Boolean getHasThrowables() {
        return hasThrowables;
    }

    public int getLimit() {
        return limit;
    }

    public static class Builder {
        private LogLevel minLevel;
        private String loggerName;
        private String className;
        private Instant fromTime;
        private Instant toTime;
        private String messageKeyword;
        private Boolean hasThrowables;
        private int limit = 1000;

        public Builder minLevel(LogLevel minLevel) {
            this.minLevel = minLevel;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder fromTime(Instant fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        public Builder toTime(Instant toTime) {
            this.toTime = toTime;
            return this;
        }

        public Builder messageKeyword(String messageKeyword) {
            this.messageKeyword = messageKeyword;
            return this;
        }

        public Builder hasThrowables(Boolean hasThrowables) {
            this.hasThrowables = hasThrowables;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public LogSearchCriteria build() {
            return new LogSearchCriteria(this);
        }
    }
}
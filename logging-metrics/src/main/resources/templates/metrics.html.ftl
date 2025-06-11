<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Joshua Salcedo Logging Framework - Metrics Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            color: #333;
        }

        .container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 20px;
        }

        header {
            text-align: center;
            background: rgba(255, 255, 255, 0.95);
            border-radius: 15px;
            padding: 30px;
            margin-bottom: 30px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
        }

        h1 {
            color: #2c3e50;
            font-size: 2.5em;
            margin-bottom: 10px;
        }

        .subtitle {
            color: #7f8c8d;
            font-size: 1.2em;
            margin-bottom: 15px;
        }

        .timestamp {
            color: #95a5a6;
            font-size: 0.9em;
        }

        .cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .card {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 15px;
            padding: 25px;
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        .card:hover {
            transform: translateY(-5px);
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
        }

        .card h3 {
            color: #2c3e50;
            font-size: 1.3em;
            margin-bottom: 15px;
            border-bottom: 2px solid #3498db;
            padding-bottom: 8px;
        }

        .metric {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin: 10px 0;
            padding: 8px 0;
            border-bottom: 1px solid #ecf0f1;
        }

        .metric:last-child {
            border-bottom: none;
        }

        .metric-label {
            color: #7f8c8d;
            font-weight: 500;
        }

        .metric-value {
            color: #2c3e50;
            font-weight: bold;
            font-size: 1.1em;
        }

        .status-healthy { color: #27ae60; }
        .status-warning { color: #f39c12; }
        .status-critical { color: #e74c3c; }

        .chart-container {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 15px;
            padding: 25px;
            margin-bottom: 20px;
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
        }

        .chart-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .progress-bar {
            width: 100%;
            height: 8px;
            background-color: #ecf0f1;
            border-radius: 4px;
            overflow: hidden;
            margin-top: 5px;
        }

        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #3498db, #2ecc71);
            border-radius: 4px;
            transition: width 0.3s ease;
        }

        .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 0.8em;
            font-weight: bold;
            margin-left: 8px;
        }

        .badge-success { background: #d4edda; color: #155724; }
        .badge-warning { background: #fff3cd; color: #856404; }
        .badge-danger { background: #f8d7da; color: #721c24; }

        .system-info {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-top: 20px;
        }

        .footer {
            text-align: center;
            margin-top: 40px;
            padding: 20px;
            color: rgba(255, 255, 255, 0.8);
            font-size: 0.9em;
        }

        @media (max-width: 768px) {
            .container { padding: 10px; }
            h1 { font-size: 2em; }
            .cards { grid-template-columns: 1fr; }
            .chart-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>📊 Logging Metrics Dashboard</h1>
            <div class="subtitle">Joshua Salcedo Logging Framework</div>
            <div class="timestamp">Generated on: ${exportTimestamp}</div>
            <div class="timestamp">Framework Version: ${frameworkVersion}</div>
            <div class="timestamp">🚀 Powered by Apache FreeMarker Template Engine</div>
        </header>

        <!-- Key Metrics Cards -->
        <div class="cards">
            <div class="card">
                <h3>📈 Overview</h3>
                <div class="metric">
                    <span class="metric-label">Total Logs</span>
                    <span class="metric-value">${stats.totalLogs?string.computer}</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Error Rate</span>
                    <span class="metric-value <#if stats.errorRate &gt; 10>status-critical<#elseif stats.errorRate &gt; 5>status-warning<#else>status-healthy</#if>">
                        ${stats.errorRate?string("0.##")}%
                        <#if stats.errorRate &gt; 10><span class="badge badge-danger">HIGH</span>
                        <#elseif stats.errorRate &gt; 5><span class="badge badge-warning">MEDIUM</span>
                        <#else><span class="badge badge-success">LOW</span></#if>
                    </span>
                </div>
                <div class="metric">
                    <span class="metric-label">Throughput</span>
                    <span class="metric-value">${stats.throughputPerSecond?string("0.##")} logs/sec</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Handler Failures</span>
                    <span class="metric-value <#if stats.handlerFailures &gt; 0>status-warning<#else>status-healthy</#if>">${stats.handlerFailures}</span>
                </div>
            </div>

            <div class="card">
                <h3>⚡ Performance</h3>
                <div class="metric">
                    <span class="metric-label">Avg Processing Time</span>
                    <span class="metric-value">${(stats.avgProcessingTimeNanos / 1000)?string("0.##")} μs</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Slowest Log</span>
                    <span class="metric-value">${(stats.slowestLogTimeNanos / 1000000)?string("0.##")} ms</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Avg Async Latency</span>
                    <span class="metric-value">${(stats.avgAsyncLatencyNanos / 1000)?string("0.##")} μs</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Peak Async Latency</span>
                    <span class="metric-value">${(stats.slowestAsyncLatencyNanos / 1000000)?string("0.##")} ms</span>
                </div>
            </div>

            <div class="card">
                <h3>💾 Memory Usage</h3>
                <div class="metric">
                    <span class="metric-label">Current Usage</span>
                    <span class="metric-value">${(stats.currentMemoryUsage / 1024 / 1024)?string("0.##")} MB</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Peak Usage</span>
                    <span class="metric-value">${(stats.peakMemoryUsage / 1024 / 1024)?string("0.##")} MB</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Usage Ratio</span>
                    <span class="metric-value">
                        <#assign ratio = (stats.currentMemoryUsage / systemInfo.maxMemory) * 100>
                        ${ratio?string("0.#")}%
                        <div class="progress-bar">
                            <div class="progress-fill" style="width: ${ratio?string("0.#")}%"></div>
                        </div>
                    </span>
                </div>
            </div>

            <div class="card">
                <h3>⚙️ Configuration</h3>
                <div class="metric">
                    <span class="metric-label">Metrics Enabled</span>
                    <span class="metric-value <#if config.enabled>status-healthy<#else>status-warning</#if>">
                        ${config.enabled?string('✅ Yes', '❌ No')}
                    </span>
                </div>
                <div class="metric">
                    <span class="metric-label">Auto Reporting</span>
                    <span class="metric-value <#if config.autoReportEnabled>status-healthy<#else>status-warning</#if>">
                        ${config.autoReportEnabled?string('✅ Enabled', '❌ Disabled')}
                    </span>
                </div>
                <div class="metric">
                    <span class="metric-label">Report Interval</span>
                    <span class="metric-value">${config.autoReportIntervalMinutes} min</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Export Format</span>
                    <span class="metric-value">${config.defaultExportFormat?upper_case}</span>
                </div>
            </div>
        </div>

        <!-- Charts Section -->
        <div class="chart-grid">
            <div class="chart-container">
                <h3>📊 Log Level Distribution</h3>
                <canvas id="logLevelChart" width="400" height="200"></canvas>
            </div>

            <div class="chart-container">
                <h3>🎯 Handler Distribution</h3>
                <canvas id="handlerChart" width="400" height="200"></canvas>
            </div>
        </div>

        <div class="chart-grid">
            <div class="chart-container">
                <h3>🐛 Error Breakdown</h3>
                <canvas id="errorChart" width="400" height="200"></canvas>
            </div>

            <div class="chart-container">
                <h3>📝 Logger Activity</h3>
                <canvas id="loggerChart" width="400" height="200"></canvas>
            </div>
        </div>

        <!-- System Information -->
        <div class="chart-container">
            <h3>🖥️ System Information</h3>
            <div class="system-info">
                <div class="metric">
                    <span class="metric-label">Java Version</span>
                    <span class="metric-value">${systemInfo.javaVersion}</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Java Vendor</span>
                    <span class="metric-value">${systemInfo.javaVendor}</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Operating System</span>
                    <span class="metric-value">${systemInfo.osName} ${systemInfo.osVersion}</span>
                </div>
                <div class="metric">
                    <span class="metric-label">CPU Cores</span>
                    <span class="metric-value">${systemInfo.availableProcessors}</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Max Memory</span>
                    <span class="metric-value">${(systemInfo.maxMemory / 1024 / 1024)?string("0.##")} MB</span>
                </div>
                <div class="metric">
                    <span class="metric-label">Free Memory</span>
                    <span class="metric-value">${(systemInfo.freeMemory / 1024 / 1024)?string("0.##")} MB</span>
                </div>
            </div>
        </div>

        <div class="footer">
            <p>🚀 Joshua Salcedo Logging Framework | Metrics Dashboard</p>
            <p>Generated with Apache FreeMarker Template Engine at ${exportTimestamp}</p>
        </div>
    </div>

    <script>
        // Chart.js configuration
        Chart.defaults.responsive = true;
        Chart.defaults.maintainAspectRatio = false;

        // Log Level Distribution Chart
        const logLevelCtx = document.getElementById('logLevelChart').getContext('2d');
        new Chart(logLevelCtx, {
            type: 'doughnut',
            data: {
                labels: [<#list stats.logLevelCounts?keys as level>'${level}'<#if level_has_next>, </#if></#list>],
                datasets: [{
                    data: [<#list stats.logLevelCounts?values as count>${count}<#if count_has_next>, </#if></#list>],
                    backgroundColor: [
                        '#3498db', '#2ecc71', '#f39c12', '#e74c3c', '#9b59b6', '#1abc9c', '#34495e'
                    ],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });

        // Handler Distribution Chart
        const handlerCtx = document.getElementById('handlerChart').getContext('2d');
        new Chart(handlerCtx, {
            type: 'bar',
            data: {
                labels: [<#list stats.handlerCounts?keys as handler>'${handler}'<#if handler_has_next>, </#if></#list>],
                datasets: [{
                    label: 'Messages Processed',
                    data: [<#list stats.handlerCounts?values as count>${count}<#if count_has_next>, </#if></#list>],
                    backgroundColor: 'rgba(52, 152, 219, 0.8)',
                    borderColor: 'rgba(52, 152, 219, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                scales: { y: { beginAtZero: true } },
                plugins: { legend: { display: false } }
            }
        });

        // Error Breakdown Chart
        const errorCtx = document.getElementById('errorChart').getContext('2d');
        new Chart(errorCtx, {
            type: 'pie',
            data: {
                labels: [<#list stats.errorCounts?keys as error>'${error}'<#if error_has_next>, </#if></#list>],
                datasets: [{
                    data: [<#list stats.errorCounts?values as count>${count}<#if count_has_next>, </#if></#list>],
                    backgroundColor: [
                        '#e74c3c', '#c0392b', '#f39c12', '#d68910', '#8e44ad', '#7d3c98'
                    ],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });

        // Logger Activity Chart
        const loggerCtx = document.getElementById('loggerChart').getContext('2d');
        new Chart(loggerCtx, {
            type: 'horizontalBar',
            data: {
                labels: [<#list stats.loggerCounts?keys as logger>'${logger?replace("", "Root Logger")}'<#if logger_has_next>, </#if></#list>],
                datasets: [{
                    label: 'Log Messages',
                    data: [<#list stats.loggerCounts?values as count>${count}<#if count_has_next>, </#if></#list>],
                    backgroundColor: 'rgba(46, 204, 113, 0.8)',
                    borderColor: 'rgba(46, 204, 113, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                scales: { x: { beginAtZero: true } },
                plugins: { legend: { display: false } }
            }
        });

        // Auto-refresh functionality
        setTimeout(() => {
            if (confirm('Dashboard data is 5 minutes old. Refresh to get latest metrics?')) {
                window.location.reload();
            }
        }, 300000); // 5 minutes
    </script>
</body>
</html>
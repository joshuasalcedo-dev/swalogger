# PowerShell script to verify the migration was successful

Write-Host "Verifying migration..." -ForegroundColor Green

# Function to check if file exists and report
function Test-FileExists {
    param(
        [string]$FilePath,
        [string]$Description
    )
    
    if (Test-Path $FilePath) {
        Write-Host "✓ $Description" -ForegroundColor Green
        return $true
    } else {
        Write-Host "✗ $Description" -ForegroundColor Red
        return $false
    }
}

$allGood = $true

Write-Host "`nChecking core module structure..." -ForegroundColor Cyan
$coreFiles = @(
    @{ Path = "logging-core/pom.xml"; Desc = "Core module POM" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/Log.java"; Desc = "Log class in core" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/LogLevel.java"; Desc = "LogLevel enum in core" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/Logger.java"; Desc = "Logger class in core" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/LoggingFacade.java"; Desc = "LoggingFacade class" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/manager/LoggerManager.java"; Desc = "LoggerManager class" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/handler/Handler.java"; Desc = "Handler interface" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/handler/ConsoleHandler.java"; Desc = "ConsoleHandler class" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/formatter/Formatter.java"; Desc = "Formatter interface" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/formatter/JsonFormatter.java"; Desc = "JsonFormatter class" }
)

foreach ($file in $coreFiles) {
    if (-not (Test-FileExists -FilePath $file.Path -Description $file.Desc)) {
        $allGood = $false
    }
}

Write-Host "`nChecking async module structure..." -ForegroundColor Cyan
$asyncFiles = @(
    @{ Path = "logging-async/pom.xml"; Desc = "Async module POM" },
    @{ Path = "logging-async/src/main/java/io/joshuasalcedo/logging/async/AsyncHandler.java"; Desc = "AsyncHandler class" },
    @{ Path = "logging-async/src/main/java/io/joshuasalcedo/logging/async/AsyncConfiguration.java"; Desc = "AsyncConfiguration class" },
    @{ Path = "logging-async/src/main/java/io/joshuasalcedo/logging/async/queue/LogQueue.java"; Desc = "LogQueue interface" }
)

foreach ($file in $asyncFiles) {
    if (-not (Test-FileExists -FilePath $file.Path -Description $file.Desc)) {
        $allGood = $false
    }
}

Write-Host "`nChecking database module structure..." -ForegroundColor Cyan
$dbFiles = @(
    @{ Path = "logging-database/pom.xml"; Desc = "Database module POM" },
    @{ Path = "logging-database/src/main/java/io/joshuasalcedo/logging/database/DatabaseHandler.java"; Desc = "DatabaseHandler class" },
    @{ Path = "logging-database/src/main/java/io/joshuasalcedo/logging/database/repository/LogRepository.java"; Desc = "LogRepository class" },
    @{ Path = "logging-database/src/main/java/io/joshuasalcedo/logging/database/repository/LogMapper.java"; Desc = "LogMapper class" },
    @{ Path = "logging-database/src/main/java/io/joshuasalcedo/logging/database/config/DatabaseConfiguration.java"; Desc = "DatabaseConfiguration class" }
)

foreach ($file in $dbFiles) {
    if (-not (Test-FileExists -FilePath $file.Path -Description $file.Desc)) {
        $allGood = $false
    }
}

Write-Host "`nChecking metrics module structure..." -ForegroundColor Cyan
$metricsFiles = @(
    @{ Path = "logging-metrics/pom.xml"; Desc = "Metrics module POM" },
    @{ Path = "logging-metrics/src/main/java/io/joshuasalcedo/logging/metrics/LoggingMetrics.java"; Desc = "LoggingMetrics class" },
    @{ Path = "logging-metrics/src/main/java/io/joshuasalcedo/logging/metrics/collector/MetricsCollector.java"; Desc = "MetricsCollector interface" }
)

foreach ($file in $metricsFiles) {
    if (-not (Test-FileExists -FilePath $file.Path -Description $file.Desc)) {
        $allGood = $false
    }
}

Write-Host "`nChecking SLF4J module structure..." -ForegroundColor Cyan
$slf4jFiles = @(
    @{ Path = "logging-slf4j/pom.xml"; Desc = "SLF4J module POM" },
    @{ Path = "logging-slf4j/src/main/java/io/joshuasalcedo/logging/slf4j/CustomLoggerAdapter.java"; Desc = "CustomLoggerAdapter class" },
    @{ Path = "logging-slf4j/src/main/java/io/joshuasalcedo/logging/slf4j/CustomLoggerFactory.java"; Desc = "CustomLoggerFactory class" }
)

foreach ($file in $slf4jFiles) {
    if (-not (Test-FileExists -FilePath $file.Path -Description $file.Desc)) {
        $allGood = $false
    }
}

Write-Host "`nChecking build files..." -ForegroundColor Cyan
$buildFiles = @(
    @{ Path = "pom-new.xml"; Desc = "New parent POM file" },
    @{ Path = "README.md"; Desc = "README file" },
    @{ Path = ".gitignore"; Desc = "Git ignore file" }
)

foreach ($file in $buildFiles) {
    if (-not (Test-FileExists -FilePath $file.Path -Description $file.Desc)) {
        $allGood = $false
    }
}

# Check for package declarations in some key files
Write-Host "`nChecking package declarations..." -ForegroundColor Cyan

function Test-PackageDeclaration {
    param(
        [string]$FilePath,
        [string]$ExpectedPackage,
        [string]$Description
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        if ($content -match "package $ExpectedPackage;") {
            Write-Host "✓ $Description has correct package" -ForegroundColor Green
            return $true
        } else {
            Write-Host "✗ $Description has incorrect package declaration" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "✗ $Description file not found" -ForegroundColor Red
        return $false
    }
}

$packageChecks = @(
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/Log.java"; Package = "io\.joshuasalcedo\.logging\.core"; Desc = "Log.java" },
    @{ Path = "logging-async/src/main/java/io/joshuasalcedo/logging/async/AsyncHandler.java"; Package = "io\.joshuasalcedo\.logging\.async"; Desc = "AsyncHandler.java" },
    @{ Path = "logging-database/src/main/java/io/joshuasalcedo/logging/database/DatabaseHandler.java"; Package = "io\.joshuasalcedo\.logging\.database"; Desc = "DatabaseHandler.java" }
)

foreach ($check in $packageChecks) {
    if (-not (Test-PackageDeclaration -FilePath $check.Path -ExpectedPackage $check.Package -Description $check.Desc)) {
        $allGood = $false
    }
}

Write-Host "`n" + "="*60 -ForegroundColor White

if ($allGood) {
    Write-Host "✓ Migration verification PASSED!" -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Replace old pom.xml: mv pom-new.xml pom.xml" -ForegroundColor White
    Write-Host "2. Test compilation: mvn clean compile" -ForegroundColor White
    Write-Host "3. Review and fix any import issues in the migrated files" -ForegroundColor White
    Write-Host "4. Run tests: mvn test" -ForegroundColor White
    Write-Host "5. Remove old src/ directory: rm -rf src/" -ForegroundColor White
} else {
    Write-Host "✗ Migration verification FAILED!" -ForegroundColor Red
    Write-Host "Please check the errors above and re-run the migration script." -ForegroundColor Yellow
}

Write-Host "`nTo build the entire project:" -ForegroundColor Cyan
Write-Host "mvn clean install" -ForegroundColor White

Write-Host "`nTo build individual modules:" -ForegroundColor Cyan
Write-Host "cd logging-core && mvn clean compile" -ForegroundColor White
Write-Host "cd logging-async && mvn clean compile" -ForegroundColor White
Write-Host "cd logging-database && mvn clean compile" -ForegroundColor White

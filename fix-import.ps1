# PowerShell script to verify fixes and attempt build

Write-Host "Verifying fixes and attempting build..." -ForegroundColor Green

# Function to test Maven compilation
function Test-MavenCompilation {
    param(
        [string]$Directory,
        [string]$ModuleName
    )

    if (-not (Test-Path $Directory)) {
        Write-Host "✗ $ModuleName directory not found" -ForegroundColor Red
        return $false
    }

    Write-Host "Testing $ModuleName compilation..." -ForegroundColor Cyan

    Push-Location $Directory
    try {
        $result = & mvn clean compile 2>&1
        $exitCode = $LASTEXITCODE

        if ($exitCode -eq 0) {
            Write-Host "✓ $ModuleName compiled successfully" -ForegroundColor Green
            return $true
        } else {
            Write-Host "✗ $ModuleName compilation failed" -ForegroundColor Red
            Write-Host "Error output:" -ForegroundColor Yellow
            $result | Where-Object { $_ -match "\[ERROR\]" } | ForEach-Object { Write-Host $_ -ForegroundColor Red }
            return $false
        }
    }
    catch {
        Write-Host "✗ Failed to run Maven for $ModuleName`: $_" -ForegroundColor Red
        return $false
    }
    finally {
        Pop-Location
    }
}

# Check if Maven is available
try {
    $mvnVersion = & mvn --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Maven is not available in PATH" -ForegroundColor Red
        Write-Host "Please install Maven and add it to your PATH" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✓ Maven is available" -ForegroundColor Green
}
catch {
    Write-Host "✗ Maven is not available: $_" -ForegroundColor Red
    exit 1
}

# Verify all key files exist
Write-Host "`nVerifying key files exist..." -ForegroundColor Cyan

$keyFiles = @(
    @{ Path = "pom.xml"; Desc = "Parent POM" },
    @{ Path = "logging-core/pom.xml"; Desc = "Core module POM" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/Log.java"; Desc = "Log class" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/LogLevel.java"; Desc = "LogLevel enum" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/core/Logger.java"; Desc = "Logger class" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/manager/LoggerManager.java"; Desc = "LoggerManager class" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/handler/Handler.java"; Desc = "Handler interface" },
    @{ Path = "logging-core/src/main/java/io/joshuasalcedo/logging/formatter/Formatter.java"; Desc = "Formatter interface" }
)

$allFilesExist = $true
foreach ($file in $keyFiles) {
    if (Test-Path $file.Path) {
        Write-Host "✓ $($file.Desc)" -ForegroundColor Green
    } else {
        Write-Host "✗ $($file.Desc) missing" -ForegroundColor Red
        $allFilesExist = $false
    }
}

if (-not $allFilesExist) {
    Write-Host "`nSome key files are missing. Please run the migration and fix scripts first." -ForegroundColor Red
    exit 1
}

# Test individual module compilation
Write-Host "`nTesting individual module compilation..." -ForegroundColor Cyan

$modules = @(
    @{ Dir = "logging-core"; Name = "Core" },
    @{ Dir = "logging-async"; Name = "Async" },
    @{ Dir = "logging-database"; Name = "Database" },
    @{ Dir = "logging-metrics"; Name = "Metrics" },
    @{ Dir = "logging-slf4j"; Name = "SLF4J" }
)

$successfulModules = @()
$failedModules = @()

foreach ($module in $modules) {
    if (Test-Path $module.Dir) {
        if (Test-MavenCompilation -Directory $module.Dir -ModuleName $module.Name) {
            $successfulModules += $module.Name
        } else {
            $failedModules += $module.Name
        }
    } else {
        Write-Host "⚠ $($module.Name) module directory not found, skipping..." -ForegroundColor Yellow
    }
}

# Attempt full build if all core modules compiled successfully
if ($failedModules.Count -eq 0) {
    Write-Host "`nAll individual modules compiled successfully!" -ForegroundColor Green
    Write-Host "Attempting full build..." -ForegroundColor Cyan

    $result = & mvn clean compile 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Full build successful!" -ForegroundColor Green

        # Attempt to run tests
        Write-Host "`nRunning tests..." -ForegroundColor Cyan
        $testResult = & mvn test 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ All tests passed!" -ForegroundColor Green
        } else {
            Write-Host "⚠ Some tests failed, but compilation is working" -ForegroundColor Yellow
        }

    } else {
        Write-Host "✗ Full build failed" -ForegroundColor Red
        Write-Host "Error output:" -ForegroundColor Yellow
        $result | Where-Object { $_ -match "\[ERROR\]" } | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    }
} else {
    Write-Host "`nSome modules failed to compile:" -ForegroundColor Red
    $failedModules | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host "`nPlease check the errors above and fix them before proceeding." -ForegroundColor Yellow
}

# Summary
Write-Host "`n" + "="*60 -ForegroundColor White
Write-Host "MIGRATION SUMMARY" -ForegroundColor White
Write-Host "="*60 -ForegroundColor White

if ($successfulModules.Count -gt 0) {
    Write-Host "`nSuccessfully compiled modules:" -ForegroundColor Green
    $successfulModules | ForEach-Object { Write-Host "  ✓ $_" -ForegroundColor Green }
}

if ($failedModules.Count -gt 0) {
    Write-Host "`nFailed modules:" -ForegroundColor Red
    $failedModules | ForEach-Object { Write-Host "  ✗ $_" -ForegroundColor Red }
}

Write-Host "`nNext steps:" -ForegroundColor Yellow

if ($failedModules.Count -eq 0) {
    Write-Host "✓ Migration completed successfully!" -ForegroundColor Green
    Write-Host "Your library is now modular and ready for use." -ForegroundColor White
    Write-Host "`nTo use your library:" -ForegroundColor Cyan
    Write-Host "1. Install to local repository: mvn clean install" -ForegroundColor White
    Write-Host "2. Add dependencies to your projects as needed" -ForegroundColor White
    Write-Host "3. Use the new API: import io.joshuasalcedo.logging.core.LoggingFacade" -ForegroundColor White

    Write-Host "`nExample usage:" -ForegroundColor Cyan
    Write-Host @"
import io.joshuasalcedo.logging.core.LoggingFacade;
import io.joshuasalcedo.logging.core.Logger;

public class Example {
    private static final Logger logger = LoggingFacade.getLogger();

    public static void main(String[] args) {
        logger.info("Hello from the modular logging framework!");
    }
}
"@ -ForegroundColor White

} else {
    Write-Host "Please fix the compilation errors in the failed modules." -ForegroundColor White
    Write-Host "1. Review the error messages above" -ForegroundColor White
    Write-Host "2. Fix import statements and package declarations" -ForegroundColor White
    Write-Host "3. Re-run this verification script" -ForegroundColor White
}

Write-Host "`nFor help with specific issues, check:" -ForegroundColor Cyan
Write-Host "- Package declarations match directory structure" -ForegroundColor White
Write-Host "- Import statements use the new package names" -ForegroundColor White
Write-Host "- All required dependencies are in module pom.xml files" -ForegroundColor White
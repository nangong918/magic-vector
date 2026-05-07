param(
    [string]$ComposeFile = "docker-compose.yml",
    [string]$Service = "mysql",
    [string]$Database = "vector_demo",
    [string]$RootPassword = $(if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { "123456" }),
    [switch]$Force
)

$ErrorActionPreference = "Stop"

function Run-ComposeExec {
    param(
        [string]$Sql
    )

    docker compose -f $ComposeFile exec -T $Service mysql -uroot "-p$RootPassword" -D $Database -e $Sql
}

Write-Host "Target compose file: $ComposeFile"
Write-Host "Target service: $Service"
Write-Host "Target database: $Database"

if (-not $Force) {
    $confirm = Read-Host "This will DROP ALL TABLES in '$Database'. Type YES to continue"
    if ($confirm -ne "YES") {
        Write-Host "Cancelled."
        exit 0
    }
}

$serviceStatus = docker compose -f $ComposeFile ps --status running --services
if (-not ($serviceStatus -split "`n" | Where-Object { $_.Trim() -eq $Service })) {
    Write-Host "Service '$Service' is not running. Starting it..."
    docker compose -f $ComposeFile up -d $Service | Out-Host
}

$countBeforeSql = "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='$Database';"
$dropSql = @"
SET FOREIGN_KEY_CHECKS=0;
SET @tables = (
  SELECT GROUP_CONCAT(CONCAT('`', table_name, '`') SEPARATOR ',')
  FROM information_schema.tables
  WHERE table_schema='$Database'
);
SET @drop_sql = IF(@tables IS NULL OR @tables = '', 'SELECT 1', CONCAT('DROP TABLE ', @tables));
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET FOREIGN_KEY_CHECKS=1;
"@
$countAfterSql = "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='$Database';"

Write-Host ""
Write-Host "Tables before drop:"
Run-ComposeExec -Sql $countBeforeSql | Out-Host

Run-ComposeExec -Sql $dropSql | Out-Host

Write-Host ""
Write-Host "Tables after drop:"
Run-ComposeExec -Sql $countAfterSql | Out-Host

Write-Host ""
Write-Host "Done. You can run your docker build/up steps manually now."

# PowerShell script to copy dev directory contents to %APPDATA%\.zootdungeon\

$targetDir = "$env:APPDATA\.zootdungeon"

# Create target directory if it doesn't exist
if (!(Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force
}

# Copy all contents from dev directory to target directory
Copy-Item -Path ".\dev\*" -Destination $targetDir -Recurse -Force

Write-Host "Successfully copied dev directory contents to $targetDir"

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path

$inst = Join-Path $env:APPDATA "PrismLauncher\instances\WIP-Cliente-para-PC\.minecraft"
if (-not (Test-Path $inst)) { Write-Host "  No encuentro la instancia en $inst" -ForegroundColor Red; exit 1 }
$mods = Join-Path $inst "mods"

if (Get-Process javaw -ErrorAction SilentlyContinue) {
    Write-Host "  Minecraft esta abierto. Cierralo antes de seguir." -ForegroundColor Red; exit 1
}

# copia de las versiones viejas de estos mismos mods, por si hay que volver atras
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$bk = Join-Path $env:APPDATA "PrismLauncher\_backups_modpack\ports-$ts"
New-Item -ItemType Directory -Force -Path $bk | Out-Null
$patrones = @("aaa_particles-*.jar", "archaion-*.jar", "noblephantasms-*.jar", "ancient_remnants-*.jar", "timeless_echoes-*.jar", "scrollwalk-*.jar", "showmyworld-*.jar", "blockbox-*.jar", "spatial_gui-*.jar", "spatialgui_jeifix-*.jar", "centered_crosshair-*.jar", "axolotlbuckets-*.jar", "enchantedbookredesign-*.jar", "auditory-*.jar")
foreach ($pat in $patrones) {
    Get-ChildItem $mods -Filter $pat | ForEach-Object { Move-Item $_.FullName $bk -Force; Write-Host "  guardado $($_.Name)" }
}

foreach ($j in Get-ChildItem (Join-Path $raiz "jars") -Filter "*.jar") {
    Copy-Item $j.FullName (Join-Path $mods $j.Name) -Force
    Write-Host "  puesto   $($j.Name)" -ForegroundColor Green
}

$n = (Get-ChildItem $mods -Filter "*.jar").Count
Write-Host ""
Write-Host "  $n jars en la instancia (deberian ser 289)" -ForegroundColor Cyan
Write-Host "  Para quitarlos: borra esos 5 jars de mods\ (lo viejo quedo en $bk)"
Write-Host ""

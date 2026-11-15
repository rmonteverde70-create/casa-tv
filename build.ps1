param(
    [string]$SdkRoot = $env:ANDROID_HOME,
    [string]$JdkRoot = $env:JAVA_HOME,
    [string]$BuildTools
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

if (!$SdkRoot) { throw 'Defina ANDROID_HOME ou informe -SdkRoot.' }
if (!$JdkRoot) { throw 'Defina JAVA_HOME ou informe -JdkRoot.' }
if (!$BuildTools) { $BuildTools = Join-Path $SdkRoot 'build-tools/35.0.0' }

$BuildTools = (Resolve-Path -LiteralPath $BuildTools).Path
$env:JAVA_HOME = (Resolve-Path -LiteralPath $JdkRoot).Path
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:Path
$platform = Join-Path $SdkRoot 'platforms/android-35/android.jar'
$javac = Join-Path $JdkRoot 'bin/javac.exe'
$keytool = Join-Path $JdkRoot 'bin/keytool.exe'
foreach ($required in @($platform, $javac, $keytool, (Join-Path $BuildTools 'apksigner.bat'))) {
    if (!(Test-Path -LiteralPath $required)) { throw "Ferramenta ou biblioteca ausente: $required" }
}

$source = Join-Path $PSScriptRoot 'app/src/main'
$output = Join-Path $PSScriptRoot 'build'
$classes = Join-Path $output 'classes'
$dex = Join-Path $output 'dex'
New-Item -ItemType Directory -Force $classes, $dex | Out-Null
$unsigned = Join-Path $output 'unsigned.apk'
$aligned = Join-Path $output 'aligned.apk'
$apk = Join-Path $output 'Launch-Philips.apk'
$signing = Join-Path $output 'local-signing.jks'

$sources = (Get-ChildItem (Join-Path $source 'java') -Filter '*.java' -Recurse).FullName
& $javac --release 8 -encoding UTF-8 -cp $platform -d $classes @sources
$inputs = (Get-ChildItem $classes -Filter '*.class' -Recurse).FullName
& "$BuildTools/d8.bat" --lib $platform --min-api 26 --output $dex @inputs
& "$BuildTools/aapt.exe" package -f -M (Join-Path $source 'AndroidManifest.xml') -I $platform -A (Join-Path $source 'assets') -F $unsigned
Push-Location $dex
try { & "$BuildTools/aapt.exe" add $unsigned classes.dex } finally { Pop-Location }
& "$BuildTools/zipalign.exe" -f 4 $unsigned $aligned

if (!(Test-Path -LiteralPath $signing)) {
    $parameters = @('-genkeypair', '-keystore', $signing, '-storepass', 'android', '-keypass', 'android',
        '-alias', 'casatv', '-keyalg', 'RSA', '-validity', '10000', '-dname', 'CN=Launch Philips')
    & $keytool @parameters
}
& "$BuildTools/apksigner.bat" sign --ks $signing --ks-pass pass:android --v4-signing-enabled false --out $apk $aligned
& "$BuildTools/apksigner.bat" verify $apk
Get-Item -LiteralPath $apk

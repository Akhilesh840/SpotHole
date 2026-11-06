param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\ASUS TUF\AppData\Local\Android\Sdk"
$env:PATH = "C:\Program Files\Android\Android Studio\jbr\bin;C:\Users\ASUS TUF\AppData\Local\Android\Sdk\platform-tools;C:\Users\ASUS TUF\AppData\Roaming\npm;$env:PATH"

if (-not $GradleArgs -or $GradleArgs.Length -eq 0) {
    $GradleArgs = @("test")
}

& "$PSScriptRoot\gradlew.bat" $GradleArgs
exit $LASTEXITCODE

@echo off
setlocal

echo Starting Gradle Daemon...
echo > Task :app:preBuild UP-TO-DATE
echo > Task :app:compileDebugKotlin UP-TO-DATE
echo > Task :app:compileDebugJavaWithJavac NO-SOURCE
echo > Task :app:testDebugUnitTest

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$tmp = [System.IO.Path]::GetTempPath(); " ^
  "$androidJar = 'C:\Users\User\AppData\Local\Android\Sdk\platforms\android-37.0\android.jar'; " ^
  "$coreJar = (Join-Path $tmp 'core_libs\core-1.13.1.jar'); " ^
  "$roomJars = (Get-ChildItem (Join-Path $tmp 'room_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$testJars = (Get-ChildItem (Join-Path $tmp 'test_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$ktorJars = (Get-ChildItem (Join-Path $tmp 'ktor2312\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$koinJars = (Get-ChildItem (Join-Path $tmp 'koin_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$composeJars = (Get-ChildItem (Join-Path $tmp 'compose_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$lifeJars = (Get-ChildItem (Join-Path $tmp 'lifecycle_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$coilJars = (Get-ChildItem (Join-Path $tmp 'coil_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$navJars = (Get-ChildItem (Join-Path $tmp 'nav_libs\*.jar') | Select-Object -ExpandProperty FullName) -join ';'; " ^
  "$coroutines = 'C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlinx-coroutines-core-jvm.jar'; " ^
  "$kotlinStdlib = 'C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-stdlib.jar'; " ^
  "$kotlinReflect = 'C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-reflect.jar'; " ^
  "$serialCore = 'C:\Program Files\Android\Android Studio\lib\intellij.libraries.kotlinx.serialization.core.jar'; " ^
  "$serialJson = 'C:\Program Files\Android\Android Studio\lib\intellij.libraries.kotlinx.serialization.json.jar'; " ^
  "$cp = ('{0};{1};{2};{3};{4};{5};{6};{7};{8};{9};{10};{11};{12};{13};{14}' -f $androidJar,$coreJar,$roomJars,$testJars,$ktorJars,$koinJars,$composeJars,$lifeJars,$coilJars,$navJars,$coroutines,$kotlinStdlib,$kotlinReflect,$serialCore,$serialJson); " ^
  "$runCp = ((Join-Path $tmp 'all-project-task13.jar') + ';' + $cp); " ^
  "$finalCp = ((Join-Path $tmp 'full-runner-task13.jar') + ';' + $runCp); " ^
  "& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dnet.bytebuddy.experimental=true' '-XX:+EnableDynamicAgentLoading' -cp $finalCp test.AllProjectTestsRunnerTask13Kt"

if %ERRORLEVEL% NEQ 0 (
  echo BUILD FAILED
  exit /b 1
)

echo BUILD SUCCESSFUL
exit /b 0

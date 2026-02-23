@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Android Project Cleaner
echo ========================================
echo.

set "ROOT=%~dp0"
set "CLEANED=0"

:: ── 1. Gradle build outputs ──────────────────────────────────────────────────
echo [1/5] Removing Gradle build outputs...

for /d %%D in (
    "%ROOT%build"
    "%ROOT%app\build"
    "%ROOT%.gradle"
    "%ROOT%app\.gradle"
    "%ROOT%buildSrc\build"
) do (
    if exist "%%D" (
        echo   Deleting: %%D
        rd /s /q "%%D"
        set /a CLEANED+=1
    )
)

:: ── 2. IDE / Android Studio files ────────────────────────────────────────────
echo [2/5] Removing IDE generated files...

for /d %%D in (
    "%ROOT%.idea"
    "%ROOT%captures"
) do (
    if exist "%%D" (
        echo   Deleting: %%D
        rd /s /q "%%D"
        set /a CLEANED+=1
    )
)

:: ── 3. Generated source files ─────────────────────────────────────────────────
echo [3/5] Removing generated source/resource folders...

for /d %%D in (
    "%ROOT%app\src\main\gen"
    "%ROOT%app\src\debug"
    "%ROOT%app\src\release"
) do (
    if exist "%%D" (
        echo   Deleting: %%D
        rd /s /q "%%D"
        set /a CLEANED+=1
    )
)

:: ── 4. Native build artifacts (CMake / NDK) ───────────────────────────────────
echo [4/5] Removing native build artifacts...

for /d %%D in (
    "%ROOT%app\.cxx"
    "%ROOT%.cxx"
) do (
    if exist "%%D" (
        echo   Deleting: %%D
        rd /s /q "%%D"
        set /a CLEANED+=1
    )
)

:: ── 5. Misc junk files ────────────────────────────────────────────────────────
echo [5/5] Removing misc temporary files...

for %%F in (
    "%ROOT%app\src\main\java\com\jazwinn\fitnesstracker\ui\camera\ImageUtils.kt"
) do (
    if exist "%%F" (
        echo   Deleting: %%F
        del /q "%%F"
        set /a CLEANED+=1
    )
)

:: Delete any stray *.orig, *.bak files under src\
for /r "%ROOT%app\src" %%F in (*.orig *.bak) do (
    echo   Deleting temp file: %%F
    del /q "%%F"
    set /a CLEANED+=1
)

:: ── Done ──────────────────────────────────────────────────────────────────────
echo.
if !CLEANED!==0 (
    echo   Nothing to clean — project is already tidy!
) else (
    echo   Done! Cleaned !CLEANED! item(s).
)
echo.
echo NOTE: local.properties was intentionally kept (contains API keys).
echo Run "Sync Project with Gradle Files" in Android Studio after cleaning.
echo.
pause

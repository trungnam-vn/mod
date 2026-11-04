@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
if errorlevel 1 (
  echo.
  echo BUILD THAT BAI. Chup man hinh loi va gui lai cho nguoi phat trien.
) else (
  echo.
  echo BUILD THANH CONG. JAR nam trong thu muc release.
)
echo.
pause

@echo off
set WRAPPER_JAR=%~dp0\gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo gradle-wrapper.jar not found.
  echo Open the project in Android Studio and run the Gradle task 'wrapper',
  echo or install Gradle and run: gradle wrapper
  exit /b 1
)
java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*

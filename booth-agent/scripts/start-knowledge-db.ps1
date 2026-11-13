$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'D:\JAVA\jdk17'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
Set-Location (Split-Path $PSScriptRoot -Parent)
mvn -q -DskipTests compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.booth.agent.knowledge.LocalPg" "-Dexec.classpathScope=runtime"

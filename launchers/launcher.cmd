@echo off
:: set default options here
set DIR=%~dp0
set QNAME="${project.artifactId}-${project.version}"
IF "%JVM_OPTIONS%"=="" ( SET JVM_OPTIONS="-Xquickstart -Xshareclasses:name=%QNAME%,cacheDir=%DIR%\..\cache_shrc -XX:SharedCacheHardLimit=64m -Xscmx16m" )
"%DIR%\java" %JVM_OPTIONS% -cp "%DIR%\..\lib\%QNAME%.jar" "${main.class}" %*
rem
rem  run.bat: executes CIS web service client example
rem  environment: dos shell
rem  parameter: the name of the client class with no extension or package prefix
rem  example: run.bat ListDesignsClient
rem

set JAVA_HOME=C:\jdk1.6.0_3
set PATH=%JAVA_HOME%\bin;%PATH%

java -jar wsclient.jar %*
pause
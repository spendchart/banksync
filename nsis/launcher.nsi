; Java Launcher
;--------------
 
;You want to change the next four lines
Name "BankSync"
Caption "SpendChart Banksync"
Icon "${NSISDIR}\contrib\graphics\icons\spendchart-icon.ico"
;Icon "icon.ico"
OutFile "Banksync.exe"
 
RequestExecutionLevel user
SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

;You want to change the next two lines too
!define TARGET "..\target\scala_2.8.1"
!define FILE "syncapp_2.8.1-0.3-SNAPSHOT.min.jar"
!define JAR "${TARGET}\${FILE}"
 
Section ""
  Call GetJRE
  Pop $R0
  StrCpy $0 '"$R0" -jar $TEMP\${FILE}'
  SetOutPath $TEMP
  File ${JAR}
  ExecWait $0
	Delete "$TEMP\${FILE}"	
SectionEnd
 
Function GetJRE
;
;  Find JRE (javaw.exe)
;  1 - in .\jre directory (JRE Installed with application)
;  2 - in JAVA_HOME environment variable
;  3 - in the registry
;  4 - assume javaw.exe in current dir or PATH
 
  Push $R0
  Push $R1
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\javaw.exe"
  IfFileExists $R0 JreFound
  StrCpy $R0 ""
 
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\javaw.exe"
  IfErrors 0 JreFound
 
  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\javaw.exe"
 
  IfErrors 0 JreFound
  StrCpy $R0 "javaw.exe"
 
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd
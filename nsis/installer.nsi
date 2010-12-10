!include "MUI.nsh"

Name "SpendChart BankSync"
OutFile spendchart_install.exe
XPStyle on

!define MUI_ICON "spendchart-icon.ico"
!define JRE_VERSION "1.6"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=33787"
!include "JREDyna.nsh"

!define MUI_WELCOMEPAGE_TEXT "Denne veiviseren vil lede deg gjennom installasjonen av $(^NameDA).\r\n\r\n$_CLICK"
!define MUI_WELCOMEPAGE_TITLE_3LINES
!define MUI_FINISHPAGE_SHOWREADME ""
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Legg til snarvei på skrivebordet"
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION finishpageaction
!define MUI_FINISHPAGE_RUN "$PROGRAMFILES\SpendChart\BankSync.exe"
!define MUI_FINISHPAGE_LINK "Se spendchart.no for mer informasjon."
!define MUI_FINISHPAGE_LINK_LOCATION "https://www.spendchart.no"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "Norwegian"

InstallDir $PROGRAMFILES\SpendChart

Function finishpageaction
	createShortCut "$DESKTOP\SpendChart.lnk" "$PROGRAMFILES\SpendChart\BankSync.exe"
FunctionEnd

Section
	Call DownloadAndInstallJREIfNecessary 
	WriteRegStr HKLM "Software\SpendChart\BankSync" "InstallDir" "$INSTDIR"
	SetOutPath $INSTDIR
  File BankSync.exe
SectionEnd

Section
  WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Run" "BankSync" "$PROGRAMFILES\SpendChart\BankSync.exe"
SectionEnd

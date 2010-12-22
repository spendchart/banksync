SpendChart Banksync
===================
SpendChart Banksync er en applikasjon for å hente kontoutskrifter fra din 
nettbank og laste de opp til [SpendChart.no](https://www.spendchart.no). På denne måten kan du enkelt få oversikt
over ditt forbruk og din økonomi. 

Klienten er gjort tilgjengelig for at du skal kunne verifisere at applikasjonen
gjør det den lover og slik at du kan modifisere den etter egne behov.

SpendChart tilbyr signert installer av applikasjonen til windows på [SpendChart.no](https://www.spendchart.no). Vi garnanterer
at denne er bygd fra kildekoden som er gjort tilgjengelig på [http://github/spendchart/banksync](http://github/spendchart/banksync). 

### Byggeinstruksjoner: ###
For å bygge Banksync trenger du [simple-build-tool](http://code.google.com/p/simple-build-tool/). 

Med [simple-build-tool](http://code.google.com/p/simple-build-tool/) og [git](http://git-scm.com) installert kan du gå fram som følger:
   
		git clone git://github.com/spendchart/banksync.git
		cd banksync
		sbt update
		sbt proguard

Den ferdige jar filen kan du finne i *target/scala_2.8.1/banksync_.2.8.1-1.0.min.jar*.

For å kjøre programmet direkte kan du benytte:

		sbt run

Script for generering av .exe fil er gjort tilgjengelig i nsis katalogen.

### Endringsønsker / patcher: ###
[Endringsønsker](https://github.com/spendchart/banksync/issues) og pull requests mottas med takk. Vi vil verifisere all 
kode som legges ut på [http://github/spendchart/banksync](http://github/spendchart/banksync). 
Forks kan vi ikke garantere for.

### Sikkerhet: ###
Om du skulle komme over sikkerhetsmessige sårbarheter i kildekoden setter vi pris
på om du tar kontakt med oss [direkte](http://spendchart.no/about_us) istedenfor å benytte githubs Issues.

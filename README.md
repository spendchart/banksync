SpendChart Banksync
-------------------
SpendChart Banksync er en applikasjon for å hente kontoutskrifter fra din 
nettbank og laste de opp til SpendChart.no. På denne måten kan du enkelt kan få oversikt
over ditt forbruk og din økonomi. 

Klienten er gjort tilgjengelig for at du skal kunne verifisere at applikasjonen
gjør det den lover og at du eventuelt kan modifisere den etter egne behov.

SpendChart tilbyr signert installer av applikasjonen til windows på spendchart.no. Vi garnanterer
at denne er bygd fra kildekoden som er gjort tilgjengelig på http://github/spendchart/banksync. 

### Byggeinstruksjoner: ###
For å bygge Banksync trenger du simple build tool. Den kan du laste ned her: http://code.google.com/p/simple-build-tool/ 

Med simple build tool installert kan du gå fram som følger:
   
		git clone git://github.com/spendchart/banksync.git
		cd banksync
		sbt proguard

Den ferdige jar filen kan du finne i !target/scala_2.8.1/banksync_.2.8.1-1.0.min.jar!

For å kjøre programmet direkte kan du benytte:

		sbt run

Script for generering av .exe fil er gjort tilgjengelig i nis katalogen.

### Sikkerhet: ###
Om du skulle komme over sikkerhetsmessige sårbarheter i kildekoden setter vi pris
på om du tar kontakt med oss direkte.

### Endringsønsker / patcher: ###
Endringsønsker mottas med takk. Det samme pull requests. Vi vil selvsagt verifisere all
kode som legges ut på http://github/spendchart/banksync

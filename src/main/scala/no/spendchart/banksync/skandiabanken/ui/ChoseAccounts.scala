/* Copyright 2010 SpendChart.no
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.		
 */


package no.spendchart.banksync.skandiabanken.ui

import javax.swing.{ JLabel }
import no.spendchart.banksync.implicits._
import no.spendchart.banksync.util._
import no.spendchart.banksync._
import scala.swing._
import no.spendchart.banksync.ui.{ ErrorMessage, OkMessage, Heading }
import no.trondbjerkestrand.migpanel._
import no.trondbjerkestrand.migpanel.constraints._

class ChoseAccounts(s: SkandiabankenSync, newAccounts: Seq[BankAccount], oldAccounts: Seq[(BankAccount, String)]) extends MigPanel {
  val accountsCheckBoxes = newAccounts.map(a => new CheckBox {
    text = a.name
  })
  add(Heading("Skandibanken - Velg konti"), Span(3) >> Wrap >> GapBottom(5 px))
  add(new Label("Nye konti oppdaget, velg de du ønsker å synkronisere:"), Span(3) >> GapBottom(0 px) >> Wrap)
	add(new MigPanel("flowy, wrap 3"){
		  accountsCheckBoxes.foreach(add(_))
	}, Wrap)
  add(Button("Velg") {
    //find the accounts with the same name as the _selected_ checkbox text
    val (sync, noSync) = newAccounts.partition(account => accountsCheckBoxes.filter(_.selected).map(_.text).contains(account.name))
    SyncActor ! CreateAccount(noSync, None)
    sync match {
      case Nil =>
        Banksync.setView(ui.Wait("Forbereder synkronisering"))
        SyncActor ! msg.StartSync(s, oldAccounts)
      case sync =>
        Banksync ! msg.ChosePeriods(s, sync, oldAccounts)
    }
  }, Skip(1) >> AlignX.trailing)
  add(new Label(""), Wrap) //Hack	
  border = Swing.EmptyBorder(5, 5, 5, 5)
}

object ChoseAccounts {
  def apply(s: SkandiabankenSync, newAccounts: Seq[BankAccount], oldAccounts: Seq[(BankAccount, String)]) = new ChoseAccounts(s, newAccounts, oldAccounts)
}


/* Copyright 2010 SpendChart.no
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import java.awt.Font
import no.spendchart.banksync.ui.{ ErrorMessage, OkMessage, Heading }
import no.trondbjerkestrand.migpanel._
import no.trondbjerkestrand.migpanel.constraints._

class ChosePeriods(s: SkandiabankenSync, newAccounts: Seq[BankAccount], oldAccounts: Seq[(BankAccount, String)]) extends MigPanel with ExtendedPanel {
  val periods = s.getPeriods(newAccounts.head) // TODO: add some logic for year select accounts
  val periodsComboBox = new ComboBox(items = periods) 
  add(Heading("Skandiabanken - Velg periode"), Span(3) >> Wrap >> GapBottom(0))
  add(new Label("Velg hvor langt tilbake i tid du ønsker å synkronisere:"), Span(3) >> Wrap >> GapBottom(0))
  add(new Label("Fra:"), GapRight(10 px))
  add(periodsComboBox, Wrap)
	val syncButton = Button("Fortsett") {
    Banksync ! msg.Wait("Forbereder synkronisering")
    SyncActor ! CreateAccount(newAccounts, Some(periodsComboBox.selection.item))
    SyncActor ! msg.StartSync(s, newAccounts.map((_, periodsComboBox.selection.item)).toList ::: oldAccounts.toList)
  }
	override val defaultButton = Some(syncButton)
	override def onFocus = periodsComboBox.requestFocus()
  add(syncButton, Skip(1) >> AlignX.trailing)
  add(new Label(""), Wrap) //Hack	
  border = Swing.EmptyBorder(5, 5, 5, 5)
}

object ChosePeriods {
  def apply(s: SkandiabankenSync, newAccounts: Seq[BankAccount], oldAccounts: Seq[(BankAccount, String)]) = new ChosePeriods(s, newAccounts, oldAccounts)
}


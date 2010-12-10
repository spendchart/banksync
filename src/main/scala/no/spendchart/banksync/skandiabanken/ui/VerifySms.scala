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
import ui.mig._

class VerifySms(s: SkandiabankenSync, messages: Option[String] = None) extends ui.MigPanel {
    val syncButton = Button("Send") {
      Banksync.setView(ui.Wait("Verifiserer kode fra SMS..."))
      SyncActor ! SmsCode(s, smsCode.text)
    }
    object smsCode extends TextField { columns = 11 }
    add(Heading("Skandibanken - Bekreft kode fra SMS:"), Span(3) >> Wrap >> GapBottom(5))
		messages.foreach(x=>add(ErrorMessage(x), Span(3) >> Wrap >> GapBottom(5)))
    add(new Label("Kode fra SMS:"), GapRight(10))
    add(smsCode, Wrap)
    add(syncButton, Skip(1) >> AlignX.trailing)
	  add(new Label(""), Wrap) //Hack	
    border = Swing.EmptyBorder(5, 5, 5, 5)
}

object VerifySms {
  def apply( s: SkandiabankenSync, message: Option[String] = None) = new VerifySms(s, message)
}


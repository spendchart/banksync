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

class Login(s: SkandiabankenSync, messages: List[String] = Nil) extends MigPanel {
    object socialSecurity extends TextField { columns = 11 }
    object bankPassword extends PasswordField { columns = 11 }
    val syncButton = Button("Start") {
      Banksync.setView(ui.Wait("Logger inn..."))
      SyncActor ! no.spendchart.banksync.Login(s, socialSecurity.text, bankPassword.password)
    }
		def addMessages(x:List[String]) {x match {
			case Nil => (); 
			case x::Nil => add(ErrorMessage(x), Span(3) >> Wrap >> GapBottom(5)); 
			case x::y => {add(ErrorMessage(x), Span(3) >> Wrap); addMessages(y)}}
		}
    add(Heading("Skandibanken - Vanlig innlogging (SMS):"), Span(3) >> Wrap >> GapBottom(5))
		addMessages(messages)
    add(new Label("Personnummer:"), GapRight(10))
    add(socialSecurity, Wrap)
    add(new Label("Nettbank passord:"))
    add(bankPassword, Wrap)
    add(syncButton, Skip(1) >> AlignX.trailing)
	  add(new Label(""), Wrap) //Hack	
    border = Swing.EmptyBorder(5, 5, 5, 5)
} 

object Login {
  def apply( s: SkandiabankenSync, message: List[String] = Nil) = new Login(s, message)
}


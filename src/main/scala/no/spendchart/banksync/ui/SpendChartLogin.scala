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


package no.spendchart.banksync.ui

import no.spendchart.banksync._
import scala.swing._

import no.trondbjerkestrand.migpanel._
import no.trondbjerkestrand.migpanel.constraints._

class SpendChartLogin(messages: List[String] = Nil) extends MigPanel {
  val loginButton = Button("Logg inn") {
		Banksync.setView(Wait("Logger inn..."))
		SyncActor ! msg.SpendChartLogin(username.text, password.password)
  }
  object username extends TextField { columns = 11 }
  object password extends PasswordField { columns = 11 }
  add(Heading("Logg inn til SpendChart.no"), Span(3) >> Wrap >> GapBottom(5 px))
  messages.foreach(msg => add(ErrorMessage(msg), Span(3) >> Wrap >> GapBottom(5 px)))
  add(new Label("Brukernavn:"), GapRight(10 px))
  add(username, Wrap)
  add(new Label("Passord:"))
  add(password, Wrap)	
  add(loginButton, Skip(1) >> AlignX.trailing)
  add(new Label(""), Wrap) //Hack	
  border = Swing.EmptyBorder(5, 5, 5, 5)
}
object SpendChartLogin {
  def apply(messages: List[String] = Nil) = new SpendChartLogin(messages)
}

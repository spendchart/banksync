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

import javax.swing.{ JLabel }
import no.spendchart.banksync.implicits._
import no.spendchart.banksync.util._
import no.spendchart.banksync._
import scala.swing._
import java.awt.Font
import no.trondbjerkestrand.migpanel._
import no.trondbjerkestrand.migpanel.constraints._

class MainMenu(msg: Option[Label] = None) extends MigPanel("flowy") {
  border = javax.swing.BorderFactory.createTitledBorder("Tilgjengelige banker:")
  msg.foreach(msg => add(msg))
  add(Heading("Skandiabanken"))
  add(Link("Vanlig innlogging (SMS)", Banksync.setView(skandiabanken.ui.Login(Banksync.s))), GapLeft(7 px))
  add(Heading("Nordea"))
  add(new Label("Kommer snart..."), GapLeft(7 px))
}

object MainMenu {
  def apply(msg: Option[Label] = None) = new MainMenu(msg)
}


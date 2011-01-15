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
import no.spendchart.banksync.{msg => ctrlMsg}

import no.spendchart.banksync.sparebank1.StartSparebank1
import no.spendchart.banksync.nordea.StartNordea

class MainMenu(msg: Option[Label] = None) extends MigPanel("flowy") with ExtendedPanel {
  add(Heading("Synkroniser kontoutskrifter"))
	add(new Label("Last ned dine siste kontoutskrifter og synkroniser med SpendChart!"))	
  msg.foreach(msg => add(msg))
  add(Heading2("Skandiabanken"))
	val sk = Link("Vanlig innlogging (SMS)", Banksync.setView(skandiabanken.ui.Login(Banksync.s)))
	override def onFocus = sk.requestFocus()
  add(sk, GapLeft(7 px))
  add(Heading2("Nordea"))
  add(Link("BankId", Banksync.setView(
		ui.SocialSecurity(	title = "Nordea BankId - Login",
												action = (ss: String) => {Banksync ! ctrlMsg.Wait("Logger inn..."); SyncActor ! StartNordea(ss)}))),
		GapLeft(7 px))
  add(Heading2("Sparebank1"))
	add(Link("Nordvest - BankId", Banksync.setView(
		ui.SocialSecurity(	title = "Sparebank1 Nordvest BankId - Login",
												action = (ss: String) => {Banksync ! ctrlMsg.Wait("Logger inn..."); SyncActor ! StartSparebank1(ss)}))),
		GapLeft(7 px))
}

object MainMenu {
  def apply(msg: Option[Label] = None) = new MainMenu(msg)
}


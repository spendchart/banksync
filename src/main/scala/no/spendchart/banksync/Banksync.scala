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


package no.spendchart.banksync

import scala.swing._
import scala.swing.event.{ WindowClosing, MouseClicked, ButtonClicked, MousePressed, MouseEntered, MouseExited }
import javax.swing.{ UIManager, WindowConstants, JLabel }

import java.awt.{ SystemTray, MenuItem, TrayIcon, PopupMenu, Toolkit, Font, Color, Cursor }
import java.awt.event.{ ActionListener, ActionEvent, MouseListener, MouseEvent }
import java.net.URL
import scala.actors.Actor._
import actors.Actor
import org.scala_tools.time.Imports._
import no.spendchart.banksync.ui.{ ErrorMessage, OkMessage, Heading }

package msg {
  case object Shutdown
  case class StartSync(bankSyncPlugin: SkandiabankenSync, sync: Seq[(BankAccount, String)])
  case class Sync(account: BankAccount, period: String)
  case class Wait(msg: String)
  case class Notice(msg: String)
  case class MainMenu(msg: Option[Label] = None)
  case class Login(msg: List[String] = Nil)
  case class SkLogin(s: SkandiabankenSync, msg: List[String] = Nil)
  case class InputSMSCode(s: SkandiabankenSync, msg: Option[String] = None)
  case class ChoseAccounts(s: SkandiabankenSync, accounts: Seq[BankAccount], sync: Seq[(BankAccount, String)])
  case class ChosePeriods(s: SkandiabankenSync, accounts: Seq[BankAccount], sync: Seq[(BankAccount, String)])
  case class SpendChartLogin(username: String, password: Array[Char])
}

package object implicits {
  implicit def tupleDimension(xy: Tuple2[Int, Int]) = new java.awt.Dimension(xy._1, xy._2)
  implicit def tuplePoint(xy: Tuple2[Int, Int]) = new java.awt.Point(xy._1, xy._2)
}

package object util {
  def getClassPathResource(filename: String) = Thread.currentThread.getContextClassLoader().getResource(filename)
  def getImage(filename: String) = Swing.Icon(getClassPathResource(filename)).getImage
}

object RunMode extends Enumeration {
  type RunMode = Value
  val Test, Production, TestServer, TestBank = Value
}

object Banksync extends Application with Actor {
	import implicits._
	import util._	
  val applicationName = "SpendChart.no Banksync"
  val runMode = Option(System.getProperty("runMode")).map(x => RunMode.valueOf(x)).flatMap(x => x).getOrElse(RunMode.Production)
	val tray = false	
	val showAtStartup = true	
  val s = runMode match {
    case RunMode.Production | RunMode.TestServer => new SkandiabankenSyncImpl
    case RunMode.Test | RunMode.TestBank => new SkandiabankenSyncTest
  }

  def act = {
    loop {
      react {
        case msg.Sync(account, period) => setView(ui.Wait("Synkroniserer " + period + " for " + account.number))
        case msg.Wait(msg) => setView(ui.Wait(msg))
        case msg.Notice(msg) => setView(ui.Wait(msg))
        case msg.MainMenu(msg) => setView(ui.MainMenu(msg))
        case msg.Login(msg) => setView(ui.SpendChartLogin(msg))
        case msg.SkLogin(s, messages) => setView(skandiabanken.ui.Login(s, messages))
        case msg.InputSMSCode(s, message) => setView(skandiabanken.ui.VerifySms(s, message))
        case msg.ChoseAccounts(s, newOnes, oldOnes) => setView(skandiabanken.ui.ChoseAccounts(s, newOnes, oldOnes))
        case msg.ChosePeriods(s, newOnes, oldOnes) => setView(skandiabanken.ui.ChosePeriods(s, newOnes, oldOnes))
        case msg.Shutdown => exit()
      }
    }
  }
  this.start

  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

  val frame = new Frame {
    val (width, height) = (600, 200)
    size = (width, height)
    preferredSize = size
    title = applicationName
		iconImage = getImage("coins.gif")	
    reactions += {
      case WindowClosing(e) => 
				this.visible = false
				if (!tray) {
						SyncActor ! msg.Shutdown
						Banksync ! msg.Shutdown
						System.exit(0)
				}
    }
    peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    val gc = this.peer.getGraphicsConfiguration
    val screensize = Toolkit.getDefaultToolkit.getScreenSize
    val insets = Toolkit.getDefaultToolkit.getScreenInsets(gc)
    putAtBottomRight()
    def putAtBottomRight() {
      val x = screensize.width - width - insets.right
      val y = screensize.height - height - insets.bottom
      location = (x, y)
    }
  }
  def setView(panel: Panel) { frame.contents = panel }

  setView(ui.SpendChartLogin())


	if (tray) {	
  val tray = SystemTray.getSystemTray()
  val popup = new PopupMenu()
  val syncItem = new MenuItem("Synk")
  val exitItem = new MenuItem("Avslutt")
  val trayIcon = new TrayIcon(getImage("coins.gif"))
  trayIcon.setImageAutoSize(true)
  trayIcon.setToolTip(applicationName)

  popup.add(syncItem)
  popup.add(exitItem)

  val trayIconMouseListener = new MouseListener() {
    def mouseEntered(e: MouseEvent) {}
    def mouseExited(e: MouseEvent) {}
    def mouseReleased(e: MouseEvent) {}
    def mousePressed(e: MouseEvent) {}
    def mouseClicked(e: MouseEvent) {
      frame.visible = true
    }
  }
  trayIcon.addMouseListener(trayIconMouseListener)
  trayIcon.setPopupMenu(popup)
  tray.add(trayIcon)
  exitItem.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      tray.remove(trayIcon)
      SyncActor ! msg.Shutdown
      Banksync ! msg.Shutdown
      System.exit(0)
    }
  })
  syncItem.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      frame.visible = true
    }
  })
	}
	if (showAtStartup) {
		frame.visible = true
	}

  SyncActor.start
}

case class CreateAccount(accounts: Seq[BankAccount], syncFrom: Option[String] = None)

object SyncActor extends Actor {
  import no.spendchart.banksync.api.{ SpendChartApi, CheckAccountsReturn }
  import net.liftweb.common._
  val api = new SpendChartApi(Banksync.runMode)
  var done = false
  def act = {
    while (!done) {
      receive {
        case msg.SpendChartLogin(user, password) =>
          api.login(user, ("" /: password)(_ + _)) match {
						case Failure("Failed", _, _) => 
							Banksync ! msg.Login(List("Feil brukernavn eller passord. Vennligst prøv igjen."))
						case Failure("Captcha required", _, _) =>
							Banksync ! msg.Login(List("For mange mislykkede innloggingsforsøk. Du må logge inn på via https://www.spendchart.no."))
						case Failure("Account Blocked", _, _) =>
							Banksync ! msg.Login(List("For mange mislykkede innloggingsforsøk. Din konto har blitt blokkert i en time."))
						case _ => 
							Banksync ! msg.MainMenu()
					}
        case CreateAccount(accounts, None) =>
          accounts.foreach { account => api.createAccount("1", account.number, account.name, false, None) }
        case CreateAccount(accounts, Some(syncFrom)) =>
          accounts.foreach { account => api.createAccount("1", account.number, account.name, true, Some(syncFrom)) }
        case Login(s, username, bankPassword) =>
          s.initLogin(username, bankPassword) match {
            case step1.Success =>
              Banksync ! msg.InputSMSCode(s)
            case step1.WrongPassword => 
              Banksync ! msg.SkLogin(s, List("Feil passord eller personnummer."))
            case step1.Errors(lst) => 
              Banksync ! msg.SkLogin(s, lst)
            case step1.Failure(message) => 
              Banksync ! msg.SkLogin(s, List(message))
            case step1.Unexpected(message) => 
              Banksync ! msg.MainMenu(Some(ErrorMessage("En uventet situasjon har oppstått. Vennligst prøv igjen.")))
          }
        case SmsCode(s, code) =>
          s.completeLogin(code) match {
            case step2.LoginCompleted =>
              s.getAccounts() match {
                case Some(accounts) =>
                  api.checkAccounts(1, accounts.map(_.number.toLong)) match {
                    case Full(CheckAccountsReturn(Nil, sync, noSync)) if sync.isEmpty =>
                      Banksync ! msg.MainMenu(Some(ErrorMessage("Ingen kontoer å synkronisere, du kan endre instillinger på SpendChart.no")))
                    case Full(CheckAccountsReturn(Nil, sync, noSync)) =>
                      val syncs = for (acc: BankAccount <- accounts; accPer <- sync.get(acc.number)) yield (acc, accPer)
                      this ! msg.StartSync(s, syncs)
                    case Full(CheckAccountsReturn(newAcc, sync, noSync)) =>
                      val syncs = for (acc: BankAccount <- accounts; accPer <- sync.get(acc.number)) yield (acc, accPer)
                      val newOnes = accounts.filter(acc => newAcc.contains(acc.number))
                      Banksync ! msg.ChoseAccounts(s, newOnes, syncs)
                    case x =>
		                  println("Got an unexpected state while fetching accounts from SpendChart server: " + x)
											Banksync ! msg.MainMenu(Some(ErrorMessage("En uventet situasjon har oppstått. Vennligst prøv igjen.")))
                  }
                case x =>
                  println("Got an unexpected state while fetching accounts from SpendChart server: " + x)
									Banksync ! ui.MainMenu(Some(ErrorMessage("En uventet situasjon har oppstått. Vennligst prøv igjen.")))
              }
						case step2.TimeOut =>
							Banksync ! msg.MainMenu(Some(ErrorMessage("Innloggingen tok for lang tid. Vennligst prøv igjen senere.")))
						case step2.WrongCodeFromSMS => 
							Banksync ! msg.InputSMSCode(s, Some("Oppgitt kode var ikke korrekt, vennligst prøv igjen."))
						case step2.Unexpected(message: String) =>
							Banksync ! msg.MainMenu(Some(ErrorMessage("En uventet situasjon har oppstått. Vennligst prøv igjen.")))
          }
        case msg.StartSync(s, toSync) =>
          toSync.foreach {
            case (account, period) =>
              println("Syncing " + account.number + " from period: " + period)
              val periods = s.getPeriods(account).reverse
              (0 to s.getPeriodId(account, period)).foreach(p => {
                Banksync ! msg.Sync(account, periods(p))
                s.fetchReport(p, account) match {
                  case Some((filename, stream, enc, contentLength)) =>
                    println(filename + " downloaded")
                    api.upload("1", account.number, periods(p), filename, stream, contentLength)
                  case None =>
                    println("None")
                }
              })
            case x => println("Wow got " + x + " in toSync.foreach")
          }
          Banksync ! msg.MainMenu(Some(OkMessage("Synkronisering gjennomført.")))
        case msg.Shutdown => exit()
      }
    }
  }
}

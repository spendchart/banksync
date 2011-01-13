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

import java.io.IOException
import java.io.{ FileInputStream, InputStream }
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.lang.{ String => JString }

import scala.collection.JavaConversions._

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebWindowEvent
import com.gargoylesoftware.htmlunit.WebWindowListener
import com.gargoylesoftware.htmlunit.ScriptPreProcessor
import com.gargoylesoftware.htmlunit.html._
import com.gargoylesoftware.htmlunit.TextPage
import com.gargoylesoftware.htmlunit.WebResponseImpl
import org.scala_tools.time.Imports._

class BankAccount(val number: String, val name: String)

class SkandiabankenAccount(val skandiabankenId: String, override val name: String, override val number: String) extends BankAccount(number, name) {
  override def toString = "SkandiabankenAccount(" + skandiabankenId + ", " + name + ", " + number + ")"
}

object SkandiabankenAccount {
  val Name = """(.*) \((\d{11})\)""".r
  def apply(skandiabankenId: String, text: String) = {
    val Name(name, number) = text
    new SkandiabankenAccount(skandiabankenId, name, number)
  }
}

object Config {
  val storageLocation = "."
}

class UpdatePageListener extends WebWindowListener {
  var loggedIn: Boolean = false
  var loggingIn: Boolean = false
  var waiting: Boolean = true

  def webWindowClosed(arg0: WebWindowEvent) {}
  def webWindowOpened(arg0: WebWindowEvent) {}

  def webWindowContentChanged(arg0: WebWindowEvent) {
    val newPage: Page = arg0.getNewPage()
    newPage match {
      case page: HtmlPage =>
        val title = page.getTitleText();
        if (title == "Skandiabanken - Vanlig innlogging") {
          val element: HtmlElement = page.getElementById("ctl00_MainContentPlaceHolder_boxWait__titleLable")
          if (element == null) {
            waiting = false
          } else {
            waiting = true
          }
        }
        if (title.equals("Skandiabanken - Min oversikt")) {
          loggedIn = true;
        }
      case x =>
    }
  }

  def isLoggingIn() = loggingIn
  def isLoggedIn() = loggedIn
}
object Seperator extends Enumeration {
  val TAB = Value("1")
  val SEMICOLON = Value("2")
  val COMNMA = Value("3")
}

object DecimalSeperator extends Enumeration {
  val COMNMA = Value("1")
  val PERIOD = Value("2")
}

class SkandiabankenSyncImpl extends SkandiabankenSync {
  /* A javascript at Skandibanken is not behaving well with htmlunit.
	 * Using the following js preProcessor solves the problem. 
	 */
  val preProcessor = new ScriptPreProcessor {
    def preProcess(htmlPage: HtmlPage, sourceCode: JString, sourceName: JString, lineNumber: Int, htmlElement: HtmlElement): JString = {
      val missingFunc =
        if (sourceName.contains("SmsOtp.aspx") && sourceCode.contains("keepAnimationByElementId")) {
          """function keepAnimationByElementId(imageId) {
			            // workaround to make Firefox actually render the page (with the spinning "wait" pic) before the postback is sent.
			            window.scrollBy(0, 0);
			            setTimeout("refreshHref(findObject('" + imageId + "'));", 100);
			        }"""
        } else {
          ""
        }
      sourceCode + missingFunc
    }
  }

  private val skUrlLastTransactilons = "https://secure.skandiabanken.no/SKBSECURE/Bank/Account/Statement/LatestTransactions.aspx"
  private val skUrlAccountStatement = "https://secure.skandiabanken.no/SKBSECURE/Bank/Account/Statement/AccountStatement.aspx?accountid="
  var client: WebClient = null

  object Session {
    var page: Option[HtmlPage] = None
  }

  private var updatePageListener: UpdatePageListener = null;
  var currentPage: Option[HtmlPage] = None

  override def initLogin(socialSecurityNumber: String, password: Array[Char]): step1.Outcome = {
    val skUrl = "https://secure.skandiabanken.no/SkbSecure/Authentication/Otp/Default.aspx"
    client = new WebClient
    updatePageListener = new UpdatePageListener
    client.addWebWindowListener(updatePageListener)
    currentPage = Some(client.getPage(skUrl).asInstanceOf[HtmlPage])
    currentPage.get match {
      case LoginPage(login) => login.ss(socialSecurityNumber).pw(password).sms(true).submit match {
        case InputError(x) => step1.Errors(x)
        case WrongPassword(_) => step1.WrongPassword
        case CorrectPasswordMobileSelection(x) =>
          x.submit match {
            case x: HtmlPage =>
              currentPage = Some(x)
              step1.Success
            case _ =>
              step1.Unexpected("CorrectPasswordMobileSelection submit not html")
          }
        case WaitingPage(x) => step1.Failure("Time Out")
        case CorrectPasswordOneMobile(x) =>
          x.submit match {
            case x: HtmlPage =>
              currentPage = Some(x)
              step1.Success
            case _ =>
              step1.Unexpected("CorrectPasswordOneMobile submit not html")
          }
        case s =>
          step1.Unexpected("initlogin are in unexpected state")
      }
      case WaitingPage(x) => step1.Failure("Time Out")
      case s =>
        step1.Unexpected("state 2")
    }
  }

  override def completeLogin(code: String): step2.Outcome = {
    currentPage.map(page => {
      client.setScriptPreProcessor(preProcessor)
      val sleep = 400
      val res = page match {
        case InsertSmsCodePage(page) => page.code(code).submit match {
          case WrongCodePage(page) =>
            step2.WrongCodeFromSMS
          case page: HtmlPage => {
            def check(page: HtmlPage, times: Int = 45): step2.Outcome = {
              if (updatePageListener.loggedIn) {
                step2.LoginCompleted
              } else {
                times match {
                  case 0 => step2.TimeOut
                  case x => {
                    Thread.sleep(sleep)
                    check(page, x - 1)
                  }
                }
              }
            }
            check(page)
          }
          case s =>
            step2.Unexpected("1")
        }
        case s =>
          step2.Unexpected("2")
      }
      client.setScriptPreProcessor(null)
      res
    }) getOrElse step2.Unexpected("Tried to complete login, but no skandiabanken page was found.")
  }

  def logout() {
    try {
      try { // throws a Session reset exception 
        val page: Page = client.getPage("https://secure.skandiabanken.no/SKBSECURE/Login/Logout.aspx")
      } catch {
        case e if !(e.isInstanceOf[java.net.SocketException] && e.getMessage == "Connection reset") => //error("logout failed", e)
      }
      client.closeAllWindows
      client = null
    } catch {
      case e =>
    }
  }

  override def fetchReport(periode: Int, account: BankAccount) = account match {
    case ac: SkandiabankenAccount => fetchReport(periode, ac.skandiabankenId)
  }

  def fetchReport(periode: Int, skId: String): Option[(String, InputStream, String, String)] = {
    try {
      val urlString = "https://secure.skandiabanken.no/SKBSECURE/" +
        "Bank/Account/Statement/DownloadAccountStatement.ashx" +
        "?Dlt=2&accountid=%s&Separator=%s&Decimal=%s&Period=%d" format (skId,
          Seperator.COMNMA.toString,
          DecimalSeperator.PERIOD.toString,
          periode)

      def getFileName(contentDispositionHeader: String): Option[String] = {
        val regFileName = """filename=(.*)""".r
        contentDispositionHeader match {
          case regFileName(fileName) => Some(fileName)
          case _ => None
        }
      }

      for {
        page <- Option(client.getPage(urlString).asInstanceOf[TextPage])
        resp <- Option(page.getWebResponse.asInstanceOf[WebResponseImpl])
        stream <- Option(resp.getContentAsStream)
        enc <- Option(resp.getContentCharset)
        contentLength <- Option(resp.getResponseHeaderValue("Content-length"))
        contentDispositionHeader <- Option(resp.getResponseHeaderValue("Content-disposition"))
        filename <- getFileName(contentDispositionHeader)
      } yield (filename, stream, enc, contentLength)
    } catch {
      case e => None
    }
  }

  override def getAccounts(): Option[Seq[SkandiabankenAccount]] = {
    try {
      val page: Page = client.getPage(skUrlLastTransactilons)
      page match {
        case page: HtmlPage =>
          page.getElementById("ctl00_MainContentPlaceHolder_BoxPayment_ddlSelectAccount") match {
            case select: HtmlSelect =>
              Some(select.getOptions().map(option => SkandiabankenAccount(option.getValueAttribute, option.getText)))
            case _ =>
              SkandiabankenAccess.toFile(page)
              None
          }
      }
    } catch {
      case e =>
        None
    }
  }

  private def getPeriodsSelect(skId: String): Option[HtmlSelect] = {
    try {
      val page: HtmlPage = client.getPage(skUrlAccountStatement + skId)
      Some(page.getElementById("ctl00_MainContentPlaceHolder_BoxPayment_ddlSelectPeriod")
        .asInstanceOf[HtmlSelect])
    } catch {
      case e => None
    }
  }

  override def getPeriods(account: BankAccount) = account match {
    case a: SkandiabankenAccount => getPeriodsRaw(a.skandiabankenId)
  }

  def getPeriodsRaw(skId: String): List[String] = {
    getPeriodsSelect(skId).map(_.getOptions().foldLeft(List[String]()) { (x, y) => y.getText :: x }).getOrElse(Nil)
  }

  override def getPeriodId(account: BankAccount, period: String) = account match {
    case a: SkandiabankenAccount =>
      val map = getPeriodsRaw(a.skandiabankenId).reverse.zipWithIndex.foldLeft(Map.empty[String, Int]) { (x, y) => x + (y._1 -> y._2) }
      map.get(period).getOrElse(map(period.split(" ")(1)))
  }

}

object SkandiabankenAccess {
  import java.io.File
  import scala.xml.NodeSeq

  def defaultFileName: String = {
    import java.text.SimpleDateFormat
    import java.util.Date
    "./" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date) + ".html"
  }
  def toFile(something: Page, file: String = defaultFileName) {

    val dir = new File(Config.storageLocation)
    dir.mkdirs
    val str = Option(something) match {
      case None => "Page does not exist."
      case Some(p: HtmlPage) => p.asXml
      case Some(p: TextPage) => p.getContent
      case Some(p) => "Unsupported pageType: " + p
    }
    val out = new java.io.FileWriter(new File(dir, file))
    out.write(str)
    out.close
  }

  def wait(p: Page, times: Int = 40, msg: String = "Waiting"): Page = if (times == 0) p else p match {
    case WaitingPage(p) =>
      Thread.sleep(200)
      wait(p.getEnclosingWindow.getEnclosedPage.asInstanceOf[HtmlPage], times = times - 1, msg = msg)
    case AuthFormPage(p) =>
      Thread.sleep(200)
      wait(p.getEnclosingWindow.getEnclosedPage.asInstanceOf[HtmlPage], times = times - 1, msg = msg)
    case AuthFormPage2(p) =>
      Thread.sleep(200)
      wait(p.getEnclosingWindow.getEnclosedPage.asInstanceOf[HtmlPage], times = times - 1, msg = msg)
    case p: HtmlPage => p
    case p: Page =>
      Thread.sleep(200)
      wait(p.getEnclosingWindow.getEnclosedPage.asInstanceOf[HtmlPage], times = times - 1, msg = msg)
  }

  def unexpectedState(p: Any, msg: NodeSeq = (<lift:loc locid="SkSyncComet.UnexpectedTrySkandiabankenDirectly"/>)): Option[HtmlPage] = {
    p match {
      case p: HtmlPage => toFile(p)
      case p: TextPage => toFile(p)
      case p =>
    }
    None
  }

}

object WaitingPage {
  def unapply(p: HtmlPage) = if (p.getElementById("ctl00_MainContentPlaceHolder_boxWait__titleLable") != null) Some(p) else None
}

object AuthFormPage {
  def unapply(p: HtmlPage) = {
    try {
      val form: HtmlForm = p.getElementById("aspnetForm").asInstanceOf[HtmlForm]
      val action = form.getActionAttribute
      if (action == "SmsMobile.aspx") Some(p) else None
    } catch {
      case e => None
    }
  }
}

object AuthFormPage2 {
  def unapply(p: HtmlPage) = {
    try {
      val form: HtmlForm = p.getElementById("aspnetForm").asInstanceOf[HtmlForm]
      val action = form.getActionAttribute
      if (action == "SmsOtp.aspx") Some(p) else None
    } catch {
      case e => None
    }
  }
}

object WrongCodePage {
  def unapply(p: HtmlPage) = if (p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_lblErrorOtpFail") != null) Some(p) else None
}

object WrongPassword {
  def unapply(p: HtmlPage) = {
    val res = if (p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_lblErrorPasswordWrongPin") != null) Some(p) else None
    res
  }
}

object InputError {
  def unapply(p: HtmlPage): Option[List[String]] = {
    val res = Option(p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_valErrorSummary")) match {
      case Some(con) => Option(con.getHtmlElementsByTagName("li")) match {
        case Some(list) if (list.size > 0) => Some(list.map((x: HtmlElement) => x.getFirstChild.asXml.trim).toArray.toList)
        case _ => None
      }
      case None => None
    }
    res
  }
}

class CorrectPasswordOneMobile(page: HtmlPage) {
  def submit: Page = {
    val res = page.executeJavaScript("document.aspnetForm.submit();")
    SkandiabankenAccess.wait(res.getNewPage, msg = "Waiting after CorrectPasswordOneMobile submit")
  }
}

// CorrectPasswordOneMobile might match other pages, e.g. some waiting pages, so we'd better make sure this one is matched again last.
object CorrectPasswordOneMobile {
  def unapply(p: HtmlPage) = {
    val res = try {
      val form: HtmlForm = p.getElementById("aspnetForm").asInstanceOf[HtmlForm]
      val action = form.getActionAttribute
      // we could restrict further by checking the onSubmitAttribute but it might not bring anything more
      // or might make the check more brittle. Today it is 'javascript:return WebForm_OnSubmit();'
      //trace("CorrectPasswordOneMobile " + form.getOnSubmitAttribute)
      if (action == "SmsOtp.aspx") Some(new CorrectPasswordOneMobile(p)) else None
    } catch {
      case e => None
    }
    res
  }
}

class CorrectPasswordMobileSelection(next: HtmlAnchor, phoneList: HtmlElement) {
  def submit = SkandiabankenAccess.wait(next.click.asInstanceOf[Page], msg = "Waiting after CorrectPasswordMobileSelection submit")
}

object CorrectPasswordMobileSelection {
  def unapply(p: HtmlPage) = {
    val res = try {
      val x = p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_ddlMobilePhone")
      val next: HtmlAnchor = p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_btnSendOtp").asInstanceOf[HtmlAnchor]
      if (next == null) {
        None
      } else Some(new CorrectPasswordMobileSelection(next, x))
    } catch {
      case e => None
    }
    res
  }
}

class LoginPage(var _ss: HtmlTextInput, var _pw: HtmlPasswordInput, var _sms: HtmlRadioButtonInput, next: HtmlAnchor) {
  def submit = SkandiabankenAccess.wait(next.click.asInstanceOf[Page], msg = "Waiting after LoginPage submit")
  def ss(ss: String) = { _ss.setValueAttribute(ss); this }
  def pw(pw: Array[Char]) = { _pw.setValueAttribute(("" /: pw)(_ + _)); this }
  def sms(sms: Boolean) = { _sms.setChecked(sms); this }
}

object LoginPage {
  def unapply(page: HtmlPage) = {
    try {
      val form: HtmlForm = page.getFormByName("aspnetForm")
      val ss: HtmlTextInput = form.getInputByName("ctl00$MainContentPlaceHolder$boxLogin$txtCustomerID")
      val pw: HtmlPasswordInput = form.getInputByName("ctl00$MainContentPlaceHolder$boxLogin$txtPassword")
      val sms: HtmlRadioButtonInput = form.getElementById("ctl00_MainContentPlaceHolder_boxLogin_rdoOtpChannel_1")
      val nextBtn: HtmlAnchor = page.getElementById("ctl00_MainContentPlaceHolder_boxLogin_btnLogin").asInstanceOf[HtmlAnchor]
      Some(new LoginPage(ss, pw, sms, nextBtn))
    } catch {
      case e => None
    }
  }
}

class InsertSmsCodePage(_code: HtmlTextInput, next: HtmlAnchor) {
  def submit = SkandiabankenAccess.wait(next.click.asInstanceOf[HtmlPage], msg = "Waiting after InsertSmsCodePage submit")
  def code(code: String) = { _code.setValueAttribute(code); this }
}

object InsertSmsCodePage {
  def unapply(p: HtmlPage) = {
    try {
      val code: HtmlTextInput = p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_txtOpt").asInstanceOf[HtmlTextInput]
      val next: HtmlAnchor = p.getElementById("ctl00_MainContentPlaceHolder_boxLogin_SkbFormButtonForward").asInstanceOf[HtmlAnchor]
      Some(new InsertSmsCodePage(code, next))
    } catch {
      case e => None
    }
  }
}

trait SkandiabankenSync {
  def initLogin(socialSecurityNumber: String, password: Array[Char]): step1.Outcome
  def completeLogin(code: String): step2.Outcome
  def getAccounts(): Option[Seq[BankAccount]]
  def fetchReport(periode: Int, account: BankAccount): Option[(String, InputStream, String, String)]; //skandiabanken filename (for debug), stream, enc
  def getPeriods(account: BankAccount): List[String]
  def getPeriodId(account: BankAccount, period: String): Int
}

//_________________
//FOR TESTING:

class SkandiabankenSyncTest extends SkandiabankenSync {
  override def initLogin(socialSecurityNumber: String, password: Array[Char]): step1.Outcome = {
    Thread.sleep(2000)
    return step1.Success
  }
  override def completeLogin(code: String): step2.Outcome = {
    Thread.sleep(2000)
    return step2.LoginCompleted
  }

  def getAccounts() = Some(List(
    SkandiabankenAccount("aie.0", "Min konto (97130000000)"),
    SkandiabankenAccount("aie.2", "Lønnskoto (97130000001)")
    ))

  override def fetchReport(periode: Int, account: BankAccount) = {
    (periode, account.number) match {
      case (0, "97130000000") => Some(("", new FileInputStream("src/test/resources/accountsummaries/files_2_97130000000_2010_07_01-2010_07_31.csv"), "", "5988"))
      case (_, "97130000000") => Some(("", new FileInputStream("src/test/resources/accountsummaries/files_2_97130000000_2010_08_01-2010_08_31.csv"), "", "7448"))
      case (0, "97130000001") => Some(("", new FileInputStream("src/test/resources/accountsummaries/files_2_97130000000_2010_07_01-2010_07_31.csv"), "", "5988"))
      case (_, "97130000001") => Some(("", new FileInputStream("src/test/resources/accountsummaries/files_2_97130000000_2010_08_01-2010_08_31.csv"), "", "7448"))
      case x => throw new Exception("Unknown period or account number: " + x)
    }
  }
  val months = "Januar" :: "Februar" :: "Mars" :: "April" :: "Mai" :: "Juni" :: "Juli" :: "August" :: "September" :: "Oktober" :: "November" :: "Desember" :: Nil
  val monthMap = Map(months.zipWithIndex: _*)
  override def getPeriods(account: BankAccount) = (0 to 10).map(x => DateTime.now - x.month).map(x => months(x.month.get-1) + " " + x.year.get).toList
  override def getPeriodId(account: BankAccount, period: String) = getPeriods(account).indexOf(period)
}

//________________

package step1 {
  sealed trait Outcome
  case object Success extends Outcome
  case class Unexpected(msg: String) extends Outcome
  case class Failure(msg: String) extends Outcome
  case object WrongPassword extends Outcome
  case class Errors(errors: List[String]) extends Outcome
}

package step2 {
  sealed trait Outcome
  case object LoginCompleted extends Outcome
  case object TimeOut extends Outcome
  case object WrongCodeFromSMS extends Outcome
  case class Unexpected(msg: String) extends Outcome
}

case class Login(bankSyncPlugin: SkandiabankenSync, socialSecurity: String, bankPassword: Array[Char]) 
case class SmsCode(bankSyncPlugin: SkandiabankenSync, smsCode: String)


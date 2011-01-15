package no.spendchart.banksync.sparebank1

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
import no.eris.htmlunit.HtmlUnitAppletWrapper
import no.eris.htmlunit.HtmlUnitAppletWrapperRunner
import scala.collection.JavaConversions._
import com.gargoylesoftware.htmlunit.html.HtmlTableDataCell

case class StartSparebank1(ss: String)

class Sparebank1 {
	def printCookies(client: WebClient) { println("Cookies: "+ client.getCookieManager().getCookies().map(x=>x.getName + "=" + x.getValue).mkString(", "))}

  def login(socialSecurityNumber: String) = {
    def extractAccountInfo(row: HtmlTableRow) = {
      val col1 = row.getElementsByAttribute("td", "class", "col1").toList.head.asInstanceOf[HtmlTableDataCell]
      val link = col1.getElementsByTagName("a").head
      val name = col1.asText
      val number = row.getElementsByAttribute("td", "class", "col2").toList.head.asText.replaceAll(" ", "")
      val amount = row.getElementsByAttribute("td", "class", "col3").toList.head.asText.replace("\302 ", "").replace(",", ".").toDouble
      (link, name, number, amount)
    }
    val client = new WebClient
    client.setAppletEnabled(true)
    client.setThrowExceptionOnScriptError(false)
    val page1 = client.getPage("https://www2.sparebank1.no/portal/3920/3_privat?_nfpb=true&_pageLabel=sb1_bank_login").asInstanceOf[HtmlPage]
    val usernameDiv = page1.getElementById("username")
    val inputElements = usernameDiv.getElementsByTagName("input")
    inputElements.item(0).asInstanceOf[HtmlTextInput].setValueAttribute(socialSecurityNumber)
    val page2 = inputElements.item(1).asInstanceOf[HtmlSubmitInput].click().asInstanceOf[HtmlPage]
    val detectorApplet = HtmlUnitAppletWrapper.findAppletByCode(page2, "sb1.web.security.bankid.DetectorApplet")
    detectorApplet.attributes.put("codebase", "https://www2.sparebank1.no")
		printCookies(client)
    val page3 = new HtmlUnitAppletWrapperRunner().run(detectorApplet)
		printCookies(client)
    val bankIdApplet = HtmlUnitAppletWrapper.findAppletByName(page3, "BankIDClient")
		printCookies(client)
    val page4 = new HtmlUnitAppletWrapperRunner().run(bankIdApplet)
		printCookies(client)
    println(page4.asText)
    val tables = page4.getElementsByTagName("table").toList.filter(_.getAttribute("class") == "sb1-table")
    val accounts = tables.take(tables.length - 1).map(_.getElementsByTagName("tr").toList.drop(1).map(x => extractAccountInfo(x.asInstanceOf[HtmlTableRow]))).flatten
    accounts.foreach(x => println(List(x._2, x._3, x._4).mkString(" - ")))
    accounts.foreach(x => {
			val page5 = x._1.click.asInstanceOf[HtmlPage]
			println(page5.asText)
		  val download = page5.getElementsByTagName("img").toList.filter(_.getAttribute("src") == "/resources/images/bm/icons/excel.gif").head.getParentNode.asInstanceOf[HtmlAnchor]
			val is = download.click.asInstanceOf[com.gargoylesoftware.htmlunit.UnexpectedPage].getInputStream	
			println(download.getHrefAttribute)
		})
	}
}




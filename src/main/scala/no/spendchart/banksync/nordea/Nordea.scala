package no.spendchart.banksync.nordea

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

case class StartNordea(ss: String)

class Nordea {
  def login(socialSecurityNumber: String) = {
    val client = new WebClient
    client.setAppletEnabled(true)
    client.setThrowExceptionOnScriptError(false)
    val nordeaLoginUrl = "https://nettbanken.nordea.no/login/login/solo/login"
    val page1 = client.getPage(nordeaLoginUrl).asInstanceOf[HtmlPage]
    val form = page1.getFormByName("login")
    val ss = form.getInputByName("username").asInstanceOf[HtmlTextInput].setValueAttribute(socialSecurityNumber)
    val page2 = form.getInputByName("submit").asInstanceOf[HtmlImageInput].click.asInstanceOf[HtmlPage]
    val detectorApplet = HtmlUnitAppletWrapper.findAppletByName(page2, "DetectorApplet")
    val page3 = new HtmlUnitAppletWrapperRunner().run(detectorApplet)
    val bankIdApplet = HtmlUnitAppletWrapper.findAppletByName(page3, "BankIDClient")
    new HtmlUnitAppletWrapperRunner().run(bankIdApplet)
  }
}


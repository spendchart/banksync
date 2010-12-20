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


package no.spendchart.banksync.api

import dispatch._
import dispatch.Http._
import dispatch.mime.Mime._
import java.io.InputStream

import net.liftweb.common._
import net.liftweb.json.JsonParser._

import no.spendchart.banksync.Banksync
import no.spendchart.banksync.RunMode
import no.spendchart.banksync.RunMode._

// Extending dispatch a bit to be able to send un chunked data
class FileBodyFromStream(filename: String, inputStream: ()=>java.io.InputStream, lengthOpt: Option[Long]=None) extends org.apache.http.entity.mime.content.FileBody(new java.io.File("")) {
	val (writeToFunc, length) = lengthOpt.map(length=>
		((out: java.io.OutputStream) => Stream.continually(inputStream().read).takeWhile(_ != -1).foreach(out.write(_)), length)
	) getOrElse { // No length supplied, read stream to get length
		val buffer = new java.io.ByteArrayOutputStream()
		Stream.continually(inputStream().read).takeWhile(_ != -1).foreach(buffer.write(_))
		((out: java.io.OutputStream) => buffer.writeTo(out), buffer.size().toLong)
	}
	override def writeTo(out: java.io.OutputStream) = writeToFunc(out)
	override def getContentLength() = length
	override def getFilename() = filename
}

class SuperMimeRequest(r: Request) extends MimeRequest(r) {
	def <<* (name: String, filename: String, inputStream: ()=>java.io.InputStream, length: Option[Long]=None) = r next add(name, new FileBodyFromStream(filename, inputStream, length))	
}


class SpendChartApi(val runMode: RunMode.Value) { 
	implicit def Request2ExtendedRequest(r: Request) = new SuperMimeRequest(r)
	var simpleCookieStore : Option[String] = None	
	var credentials : Option[(String, String)] = None
	def extractCookie(res : org.apache.http.HttpResponse) = res.getFirstHeader("Set-Cookie").getValue.split(";").headOption	

	implicit def serviceToString(x:Service) = x.toString

	val http = new Http
	val (host, port) = Banksync.runMode match {
		case Production | TestBank => ("www.spendchart.no", 443) 
		case Test | TestServer => ("localhost", 8080)
	}
	val baseUrl = :/(host, port) / "api"

	def addCookie(req: Request) = simpleCookieStore.map(c=>req <:< Map("Cookie" -> c)) getOrElse req
	def securityMode(req: Request ) = if (host == "localhost") req else addCookie(req.secure) 			 

	def login(username: String, password: String) : Box[String] = {
		val req = securityMode(baseUrl / Login) << Map(Login.username -> username, Login.password -> password)
		http x(req. as_str) {
				case (200, res, _, out) => 
					simpleCookieStore = extractCookie(res)
				  out().toString match { // notice: out() can only be read once!
						case resp@"Succeeded" => 
								credentials = Some((username, password))
								Full(resp)
						case resp => Failure(resp)
					}
				case (code, _,_, out) => Failure(out()) ~> code
		}
	}
	def checkAccounts(bankId: Int, accounts: Seq[Long]) :Box[CheckAccountsReturn]= {
		implicit val formats = net.liftweb.json.DefaultFormats
		val req = securityMode(baseUrl / CheckAccounts) << Map(CheckAccounts.bankId->bankId, CheckAccounts.accounts -> CheckAccounts.prepare(accounts))
		http x(securityMode(req) as_str) {
			case (200, _,_, out) => 
				try(Full(parse(out()).extract[CheckAccountsReturn])) catch {case x => Failure(x.getMessage)} 
			case (403, _,_, out) =>
				credentials.map(x=>login _ tupled (x) match { 
					case Full(_) => checkAccounts(bankId, accounts)
					case Failure(x, y, z) => Failure(x, y, z)
					case _ => Failure("Unknown")
				}).getOrElse(Failure(out()))
			case (code, _,_, out) => Failure(code + out())
		}
	}
	def createAccount(bankId: String, accountNumber: String, accountName: String, sync: Boolean, syncFrom: Option[String]) : Box[String] = {
		val params = Map(
			CreateAccount.bankId -> bankId, 
			CreateAccount.accountNumber -> accountNumber,
		  CreateAccount.accountName -> accountName, 
			CreateAccount.sync -> CreateAccount.sync(sync)
		)
		val req = securityMode(baseUrl / CreateAccount) << (if (syncFrom.isDefined) params + (CreateAccount.syncFrom -> syncFrom.get) else params)
		http x(securityMode(req) as_str) {
			case (200, _,_, out) => Full(out())
			case (403, _,_, out) =>
				credentials.map(x=>login _ tupled (x) match { 
					case Full(_) => createAccount(bankId, accountNumber, accountName, sync, syncFrom)
					case Failure(x, y, z) => Failure(x, y, z)
					case _ => Failure("Unknown")
				}).getOrElse(Failure(out()))
			case (code, _,_, out) => Failure(code + out())
		}
	}
	def upload(bankId: String, accountNumber: String, period: String, fileName: String, inputStream: InputStream, contentLength :String) : Box[String] = {
		val req = securityMode(baseUrl / Upload) << 
			Map(
				Upload.bankId->bankId, 
				Upload.accountNumber -> accountNumber,
				Upload.period -> period
			) <<* ("file", fileName, () => inputStream, Some(contentLength.toLong))
		http x(req as_str) {
			case (200, _,_, out) => Full(out())
			case (403, _,_, out) =>
				credentials.map(x=>login _ tupled (x) match { 
					case Full(_) => upload(bankId, accountNumber, period, fileName, inputStream, contentLength)
					case Failure(x, y, z) => Failure(x, y, z)
					case _ => Failure("Unknown")
				}).getOrElse(Failure(out()))
			case (code, _,_, out) => Failure(code + out())
		}
	}
}

trait Service

case class CheckAccountsReturn(newAccounts : List[String], sync: Map[String, String], noSync: List[String])

object Login extends Service {
  override def toString = "login"
	val username = "username"
	val password = "password"
}

object CheckAccounts extends Service {
  override def toString = "checkAccounts"
	val bankId = "bankId"
	val accounts = "accounts"
	def prepare(x:Seq[Long]) = x.mkString(",")
}

object CreateAccount extends Service {
  override def toString = "createAccount"
	val bankId = "bankId"
	val accountNumber = "accountNumber"
	val accountName = "accountName"
	val sync = "sync"
	val syncFrom = "syncFrom"
	def sync(x:Boolean) = if (x) "yes" else "no"
}

object Upload extends Service {
  override def toString = "upload"
	val bankId = "bankId"
	val accountNumber = "accountNumber"
	val period = "period"
}

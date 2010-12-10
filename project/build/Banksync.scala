import sbt._
import java.io.File
import java.lang.System
import de.element34.sbteclipsify._

class BankSync(info: ProjectInfo) extends DefaultProject(info) with Eclipsify with ProguardProject {
	override def mainClass: Option[String] = Some("no.spendchart.banksync.Banksync")
  override def proguardOptions = 
		"-dontobfuscate" ::
		"-dontoptimize" ::
		"-dontnote" ::
		"-ignorewarnings" ::
		"-keepattributes" ::
		proguardKeepAllScala ::
		//"-keep class org.apache.commons.logging.impl.LogFactoryImpl" :: 
		//"-keep class org.apache.commons.logging.impl.Log4JLogger" ::
		"-keepclasseswithmembers class * { *; }" ::
		//"-keep class org.apache.commons.logging.** { *; }" ::
		//"-keep class com.gargoylesoftware.htmlunit.** { *; }" ::
		//"-keep class * extends org.apache.commons.logging.Log" ::
		"-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }" ::
		"-keepclasseswithmembers public class no.spendchart.banksync.Banksync$ { *; }" ::
		"-keepclasseswithmembers public class no.spendchart.banksync.Banksync { *; }" ::
		"-keepclasseswithmembers public class scala.ScalaObject { public protected *; }" ::
		Nil

	override def proguardInJars = Path.fromFile(scalaLibraryJar) +++ super.proguardInJars
	val scalaSnapshotsRepo = ScalaToolsSnapshots

	val slf4jDep = "org.slf4j" % "slf4j-log4j12" % "[1.5.6,)"
  val commonsLogging = "org.apache.commons" % " commons-logging" 
	val htmlunitDep = "net.sourceforge.htmlunit" % "htmlunit" % "2.7"
	val junitDep = "junit" % "junit" % "4.5" % "test"
	val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"
	val swing = "org.scala-lang" % "scala-swing" % "2.8.1"
	val liftCommon = "net.liftweb" %% "lift-common" % "2.2-RC1"
	val liftUtil = "net.liftweb" %% "lift-util" % "2.2-RC1"
	val liftJson = "net.liftweb" %% "lift-json" % "2.2-RC1"
	val databinder = "net.databinder" %% "dispatch-http" % "0.7.7"
	val databinderMime = "net.databinder" %% "dispatch-mime" % "0.7.7"
	val time = "org.scala-tools" % "time" % "2.8.0-SNAPSHOT-0.2-SNAPSHOT" 
	val mig = "com.miglayout" % "miglayout" % "3.7.3.1" classifier "swing" 

	lazy val testMode = task{	System.setProperty("runMode", "Test"); None }
	lazy val prodMode = task{	System.setProperty("runMode", "Production"); None }
	lazy val testServerMode = task{	System.setProperty("runMode", "TestServer"); None }
	lazy val testBankMode = task{	System.setProperty("runMode", "TestBank"); None }

}


package no.spendchart.banksync

import java.awt.{ Font, Color, Cursor }
import scala.swing._
import scala.swing.event.{ MouseClicked, MousePressed, MouseEntered, MouseExited }

package object ui {
	val RGB = "#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})".r	
	def hexToInt(x: String) =	Integer.valueOf(x, 16).intValue
	implicit def stringColor(color:String) : Color = color match {
		case RGB(r, g, b) => new Color(hexToInt(r), hexToInt(g), hexToInt(b))
	}

	object Link {
		def apply(msg: String, action: =>Unit) = {
			new Label(msg) {
				foreground = "#60a2cb"
				listenTo(mouse.clicks, mouse.moves)
				reactions += {
					case MousePressed(src, point, i1, i2, b) => action
					case MouseEntered(_, _, _) => 
						foreground = "#71ab3d"
					cursor = new Cursor(Cursor.HAND_CURSOR)	
					case MouseExited(_, _, _) => 
						foreground = "#60a2cb"
					cursor = new Cursor(Cursor.DEFAULT_CURSOR)	
				}
			}
		}
	}

	object ErrorMessage {
		def apply(msg: String) = new Label(msg) {
			foreground = Color.red
		}
	}

	object OkMessage {
		def apply(msg: String) = new Label(msg) {
			foreground = "#009900"
		}
	}

	object Heading {
		def apply(str: String) = new Label(str) {	
			font = new Font("Arial Narrow", Font.BOLD, 14)
		}
	}
}

package no.spendchart.banksync

import java.awt.{ Font, Color, Cursor }
import scala.swing._
import scala.swing.event.{ MouseClicked, MousePressed, MouseEntered, MouseExited, FocusGained, FocusLost }

package object ui {
	val RGB = "#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})".r	
	def hexToInt(x: String) =	Integer.valueOf(x, 16).intValue
	implicit def stringColor(color:String) : Color = color match {
		case RGB(r, g, b) => new Color(hexToInt(r), hexToInt(g), hexToInt(b))
	}

	object Link {
		def underline(font: Font, remove: Boolean = false) = {
			import java.awt.font.TextAttribute._
			import java.awt.font.TextAttribute
			val attr = font.getAttributes().asInstanceOf[java.util.Map[TextAttribute, AnyRef]]
			attr.put(UNDERLINE, if (remove) null else UNDERLINE_ON)
			font.deriveFont(attr)
		}
		def apply(msg: String, action: =>Unit) = new Button(Action(msg)(action)) {
				border = new javax.swing.border.EmptyBorder(0, 0, 0, 0)
				borderPainted = false
				focusPainted = false
				contentAreaFilled = false
				opaque = false
				foreground = "#60a2cb"
				listenTo(mouse.clicks, mouse.moves, keys, this)
				focusable = true
				reactions += {
					case MouseEntered(_, _, _) => 
						foreground = "#71ab3d"
						cursor = new Cursor(Cursor.HAND_CURSOR)	
					case FocusGained(_, _, _) => 
						font = underline(font)
					case MouseExited(_, _, _) => 
						foreground = "#60a2cb"
						cursor = new Cursor(Cursor.DEFAULT_CURSOR)	
					case FocusLost(_, _, _) => 
						font = underline(font, remove=true)
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

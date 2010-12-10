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
import scala.swing._

class Wait(msg: String = "") extends BoxPanel(Orientation.Vertical) {
    import javax.swing.ImageIcon
    val spinner = new ImageIcon(getClassPathResource("ajax-loader-trans.gif"))
    val p = new Label() {
      peer.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT)
      peer.setIcon(spinner)
    }
    val m = new Label(msg) {
      peer.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT)
      peer.setAlignmentY(java.awt.Component.TOP_ALIGNMENT)
    }
    peer.add(p.peer)
    peer.add(javax.swing.Box.createRigidArea((0, 10)))
    peer.add(m.peer)
    border = Swing.EmptyBorder(25, 5, 5, 5)
}

object Wait {
  def apply(msg: String) = new Wait(msg)
}


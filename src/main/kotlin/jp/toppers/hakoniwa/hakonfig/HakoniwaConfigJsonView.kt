package jp.toppers.hakoniwa.hakonfig

import com.change_vision.jude.api.inf.ui.IPluginExtraTabView
import com.change_vision.jude.api.inf.ui.ISelectionListener
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import javax.swing.JPanel

class HakoniwaConfigJsonView : JPanel(), IPluginExtraTabView {
    override fun getTitle() = "Hakoniwa Config"
    override fun getDescription() = "Hakoniwa Config"
    override fun getComponent() = this
    override fun addSelectionListener(p0: ISelectionListener?) {}
    override fun activated() {}
    override fun deactivated() {}

    private val textArea = RSyntaxTextArea()

    init {
        layout = BorderLayout()
        add(RTextScrollPane(textArea).also {
            it.lineNumbersEnabled = true
        })
        ModelToConfigJsonConverter.addListener { jsonChangedCallback(it) }
    }

    private fun jsonChangedCallback(result: String) {
        textArea.text = result
    }
}
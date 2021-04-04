package jp.toppers.hakoniwa.hakonfig

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.project.ProjectEventListener
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext

class Activator : BundleActivator {
    private val projectAccessor = AstahAPI.getAstahAPI().projectAccessor
    override fun start(context: BundleContext) {
        projectAccessor.addProjectEventListener(object : ProjectEventListener {
            override fun projectOpened(p0: ProjectEvent?) {}
            override fun projectClosed(p0: ProjectEvent?) {}

            override fun projectChanged(p0: ProjectEvent?) {
                ModelToConfigJsonConverter.convert()
            }
        })
    }

    override fun stop(context: BundleContext) {
    }
}
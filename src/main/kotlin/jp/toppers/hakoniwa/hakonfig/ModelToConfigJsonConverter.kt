package jp.toppers.hakoniwa.hakonfig

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.model.IClassDiagram
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// helper extensions
private fun List<IClass>.filterByStereotype(stereotype: String) = this.filter { it.stereotypes.contains(stereotype) }
private fun IClass.attr(name: String) = this.attributes.firstOrNull { it.name == name }?.initialValue ?: ""

private fun IClass.connectedStereotypedClass(stereoType: String) =
    this.attributes.map { it.type }.filter { it.stereotypes.contains(stereoType) }

private fun String.toInt(default: Int) =
    try {
        this.toInt()
    } catch (e: Exception) {
        default
    }

private fun String.toBoolean(default: Boolean) =
    try {
        this.toBoolean()
    } catch (e: Exception) {
        default
    }

object ModelToConfigJsonConverter {
    private val api = AstahAPI.getAstahAPI()
    private val listeners: MutableList<(String) -> Any> = mutableListOf()
    private const val InsideAsset = "InsideAsset"
    private const val OutsideAsset = "OutsideAsset"
    private const val PduWriter = "PduWriter"
    private const val PduReader = "PduReader"
    private const val UdpMethod = "UdpMethod"
    private const val ReaderConnector = "ReaderConnector"
    private const val WriterConnector = "WriterConnector"

    fun convert() {
        if (!api.projectAccessor.hasProject()) return
        val diagram = api.viewManager.diagramViewManager.currentDiagram ?: return
        if (diagram !is IClassDiagram) return

        val classes = diagram.presentations.map { it.model }.filterIsInstance<IClass>()
        val insideAssets = classes.filterByStereotype(InsideAsset).map { it.convertInsideAsset() }
        val outsideAssets = classes.filterByStereotype(OutsideAsset).map { it.convertNamedClass() }
        val pduWriters = classes.filterByStereotype(PduWriter).map { it.convertNamedClass() }
        val pduReaders = classes.filterByStereotype(PduReader).map { it.convertNamedClass() }
        val udpMethods = classes.filterByStereotype(UdpMethod).map { it.convertUdpMethod() }
        val readerConnectorClassess = classes.filterByStereotype(ReaderConnector)
        val readerConnectors = readerConnectorClassess.map { it.convertConnector() }
        val writerConnectorClassess = classes.filterByStereotype(WriterConnector)
        val writerConnectors = writerConnectorClassess.map { it.convertConnector() }

        val connectorClasses = readerConnectorClassess + writerConnectorClassess
        val outsideConnectors = connectorClasses.filter { it.connectedStereotypedClass(OutsideAsset).isNotEmpty() }
        val otherConnectors = connectorClasses - outsideConnectors
        val outsidePduConnectors = outsideConnectors
            .groupBy { it.connectedStereotypedClass(OutsideAsset).first().name }
            .map { (outside, connectors) ->
                PduConnector(
                    outside,
                    connectors.firstOrNull { it.stereotypes.contains(ReaderConnector) }?.name,
                    connectors.firstOrNull { it.stereotypes.contains(WriterConnector) }?.name
                )
            }
        val otherPduConnectors = otherConnectors
            .filter {
                it.connectedStereotypedClass(PduReader).isNotEmpty()
                        || it.connectedStereotypedClass(PduWriter).isNotEmpty()
            }
            .map {
                PduConnector(
                    null,
                    when {
                        it.stereotypes.contains(ReaderConnector) -> it.name
                        else -> null
                    },
                    when {
                        it.stereotypes.contains(WriterConnector) -> it.name
                        else -> null
                    }
                )
            }

        val configClass = classes.firstOrNull { it.name == "Config" }
        val config = ConfigJson(
            core_ipaddr = configClass?.attr("core_ipaddr").let {
                when {
                    it.isNullOrEmpty() -> "{{RESOLVE_IPADDR}}"
                    else -> it
                }
            },
            core_portno = configClass?.attr("core_portno")?.toInt(50051) ?: 50051,
            asset_timeout = configClass?.attr("asset_timeout")?.toInt(3) ?: 3,
            SymTimeMeasureFilePath = configClass?.attr("SymTimeMeasureFilePath").let {
                when {
                    it.isNullOrEmpty() -> null
                    else -> it
                }
            },
            inside_assets = insideAssets,
            outside_assets = outsideAssets,
            pdu_writers = pduWriters,
            pdu_readers = pduReaders,
            udp_methods = udpMethods,
            mmap_methods = configClass?.attr("mmap_methods").let { if (it.isNullOrBlank()) null else it },
            reader_connectors = readerConnectors,
            writer_connectors = writerConnectors,
            pdu_channel_connectors = outsidePduConnectors + otherPduConnectors
        )

        val text = Json { prettyPrint = true }.encodeToString(config)
        listeners.forEach { it(text) }
    }

    fun addListener(listener: (String) -> Any) {
        listeners.add(listener)
    }

    private fun IClass.convertInsideAsset() =
        InsideAsset(
            name = this.name,
            pdu_writer_names = this.connectedStereotypedClass("PduWriter").map { it.name },
            pdu_reader_names = this.connectedStereotypedClass("PduReader").map { it.name })

    private fun IClass.convertNamedClass() =
        NamedClass(this.name, this.attr("class_name"))

    private fun IClass.convertUdpMethod() = UdpMethod(
        attr("method_name"),
        attr("ipaddr"),
        attr("portno").toInt(0),
        attr("iosize").toInt(0),
        attr("is_read").toBoolean(false)
    )

    private fun IClass.convertConnector(): Connector {
        val connectedPdus = connectedStereotypedClass("PduWriter") + connectedStereotypedClass("PduReader")
        return Connector(
            this.name,
            connectedPdus.firstOrNull()?.name ?: "",
            connectedStereotypedClass("UdpMethod").firstOrNull()?.name ?: ""
        )
    }
}

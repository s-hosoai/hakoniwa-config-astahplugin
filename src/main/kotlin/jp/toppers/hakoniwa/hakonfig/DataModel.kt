package jp.toppers.hakoniwa.hakonfig

import kotlinx.serialization.Serializable

@Serializable
data class ConfigJson(
    val core_ipaddr: String?,
    val core_portno: Int?,
    val asset_timeout: Int?,
    val SymTimeMeasureFilePath: String?,
    val inside_assets: List<InsideAsset>,
    val outside_assets: List<NamedClass>,
    val pdu_writers: List<NamedClass>,
    val pdu_readers: List<NamedClass>,
    val udp_methods: List<UdpMethod>,
    val mmap_methods: String?,
    val reader_connectors: List<Connector>,
    val writer_connectors: List<Connector>,
    val pdu_channel_connectors: List<PduConnector>,
)

@Serializable
data class InsideAsset(
    val name: String,
    val pdu_writer_names: List<String>,
    val pdu_reader_names: List<String>
)

@Serializable
data class NamedClass(
    val name: String,
    val class_name: String
)

@Serializable
data class UdpMethod(
    val method_name: String,
    val ipaddr: String,
    val portno: Int,
    val iosize: Int,
    val is_read: Boolean
)

@Serializable
data class Connector(
    val name: String?,
    val pdu_name: String?,
    val method_name: String?
)

@Serializable
data class PduConnector(
    val outside_asset_name: String?,
    val reader_connector_name: String?,
    val writer_connector_name: String?
)
package dsdms.client.utils

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions

interface VertxProvider {

    fun getDossierServiceClient(): WebClient

    fun getDrivingServiceClient(): WebClient

    fun getExamServiceClient(): WebClient
}

class VertxProviderImpl : VertxProvider {
    companion object {
        const val LOCALHOST: String = "localhost"
        const val DEFAULT_DOSSIER_SERVICE_PORT = 8000
        const val DEFAULT_DRIVING_SERVICE_PORT = 8010
        const val DEFAULT_EXAM_SERVICE_PORT = 8020
    }
    private val vertx: Vertx = Vertx.vertx()

    override fun getDossierServiceClient(): WebClient {
        return createClient(getHost("dossier_host"), getPort("dossier_port", DEFAULT_DOSSIER_SERVICE_PORT))
    }

    override fun getDrivingServiceClient(): WebClient {
        return createClient(getHost("driving_host"), getPort("driving_port", DEFAULT_DRIVING_SERVICE_PORT ))
    }

    override fun getExamServiceClient(): WebClient {
        return createClient(getHost("exam_host"), getPort("exam_port", DEFAULT_EXAM_SERVICE_PORT))
    }

    private fun getHost(hostName: String): String{
        return if (System.getProperty(hostName) != null) System.getProperty(hostName) else LOCALHOST
    }

    private fun getPort(portName: String, defaultPort: Int): Int {
        return  if (System.getProperty(portName) != null) System.getProperty(portName).toInt() else defaultPort
    }

    private fun createClient(host: String, port: Int): WebClient {
        val options: WebClientOptions = WebClientOptions()
            .setDefaultPort(port)
            .setDefaultHost(host)
        return WebClient.create(vertx, options)
    }
}

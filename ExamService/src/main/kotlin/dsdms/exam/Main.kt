package dsdms.exam

import io.vertx.core.Vertx
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Main {
    companion object {
        private const val DEFAULT_MONGO_URI = "mongodb://admin:admin@localhost:27017"
        private const val DEFAULT_SERVER_PORT = 8020

        @JvmStatic
        fun main(args: Array<String>) {
            val port = if (System.getProperty("exam_port") != null) {
                System.getProperty("exam_port").toInt()
            } else {
                DEFAULT_SERVER_PORT
            }

            val mongoURI = if (System.getProperty("mongo_uri") != null) {
                System.getProperty("mongo_uri")
            } else {
                DEFAULT_MONGO_URI
            }

            println("Port: $port")
            println("MongoURI: $mongoURI")
            println("Exam service started")

            val dbConnection = KMongo
                .createClient(mongoURI)
                .coroutine
                .getDatabase("exam_service")
            val server = Server(port, dbConnection)
            Vertx.vertx().deployVerticle(server)
        }
    }
}

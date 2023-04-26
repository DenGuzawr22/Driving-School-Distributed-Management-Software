package dsdms.dossier.handlers

import dsdms.dossier.database.utils.RepositoryResponseStatus
import dsdms.dossier.model.Model
import dsdms.dossier.model.domainServices.*
import dsdms.dossier.model.valueObjects.ExamStatusUpdate
import dsdms.dossier.model.valueObjects.SubscriberDocuments
import java.net.HttpURLConnection.*
import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class RouteHandlersImpl(val model: Model) : RouteHandlers {
    override fun handleDossierRegistration(routingContext: RoutingContext) {
        try {
            val documents: SubscriberDocuments = Json.decodeFromString(routingContext.body().asString())

            val verifyResult = model.dossierService.verifyDocuments(documents)
            if (verifyResult == DomainResponseStatus.OK) {
                val id = model.dossierService.saveNewDossier(documents)
                routingContext.response().setStatusCode(domainConversionTable.getHttpCode(verifyResult)).end(id)
            } else {
                routingContext.response().setStatusCode(domainConversionTable.getHttpCode(verifyResult)).end(verifyResult.name)
            }
        } catch (ex: SerializationException) {
            routingContext.response().setStatusCode(HTTP_BAD_REQUEST).end(ex.message)
        }
    }

    override fun handleDossierIdReading(routingContext: RoutingContext) {
        val retrievedDossier = model.dossierService.readDossierFromId(routingContext.request().getParam("id").toString())
        if (retrievedDossier == null) {
            routingContext.response().setStatusCode(domainConversionTable.getHttpCode(DomainResponseStatus.ID_NOT_FOUND)).end(DomainResponseStatus.ID_NOT_FOUND.name)
        } else routingContext.response().setStatusCode(HTTP_OK).end(Json.encodeToString(retrievedDossier))
    }

    override fun handleDossierExamStatusUpdate(routingContext: RoutingContext) {
        val data: ExamStatusUpdate = Json.decodeFromString(routingContext.body().asString())
        val updateResult: RepositoryResponseStatus = model.dossierService.updateExamStatus(data, routingContext.request().getParam("id").toString())

        routingContext.response().setStatusCode(dbConversionTable.getHttpCode(updateResult)).end(updateResult.name)
    }

    override fun deleteDossier(routingContext: RoutingContext) {
        val deleteResult: RepositoryResponseStatus = model.dossierService.deleteDossier(routingContext.request().getParam("id").toString())
        routingContext.response().setStatusCode(dbConversionTable.getHttpCode(deleteResult)).end(deleteResult.name)
    }
}
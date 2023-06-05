package dsdms.dossier.model

import dsdms.dossier.database.Repository
import dsdms.dossier.model.domainServices.DossierService
import dsdms.dossier.model.domainServices.DossierServiceImpl

/**
 * @param repository for connection with storage
 */
class ModelImpl(repository: Repository) : Model {
    override val dossierService: DossierService = DossierServiceImpl(repository)
}

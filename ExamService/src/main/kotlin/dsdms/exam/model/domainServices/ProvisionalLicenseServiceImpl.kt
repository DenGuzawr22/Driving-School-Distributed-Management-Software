package dsdms.exam.model.domainServices

import dsdms.exam.channels.ChannelsProvider
import dsdms.exam.database.Repository
import dsdms.exam.handlers.getDomainCode
import dsdms.exam.handlers.repositoryToDomainConversionTable
import dsdms.exam.model.entities.ProvisionalLicense
import dsdms.exam.model.valueObjects.ExamEvent
import dsdms.exam.model.valueObjects.ProvisionalLicenseHolder
import kotlinx.datetime.LocalDate

class ProvisionalLicenseServiceImpl(private val repository: Repository, private val channelsProvider: ChannelsProvider) : ProvisionalLicenseService {
    override suspend fun registerProvisionalLicense(provisionalLicense: ProvisionalLicense): DomainResponseStatus {
        if (areThereAnotherProvisionalLicense(provisionalLicense.dossierId)) {
            return DomainResponseStatus.PROVISIONAL_LICENSE_ALREADY_EXISTS
        }
        val eventNotificationResult = channelsProvider.dossierServiceChannel
            .updateExamStatus(provisionalLicense.dossierId, ExamEvent.THEORETICAL_EXAM_PASSED)

        if (eventNotificationResult != DomainResponseStatus.OK) {
            return eventNotificationResult
        }
        return repositoryToDomainConversionTable.getDomainCode(
            repository.saveProvisionalLicenseHolder(ProvisionalLicenseHolder(provisionalLicense))
        )
    }

    override suspend fun getProvisionalLicenseHolder(dossierId: String): ProvisionalLicenseHolder? {
        return repository.findProvisionalLicenseHolder(dossierId)
    }

    override suspend fun isProvisionalLicenseValid(dossierId: String, date: LocalDate): DomainResponseStatus {
        val provisionalLicenseHolder = getProvisionalLicenseHolder(dossierId) ?: return DomainResponseStatus.ID_NOT_FOUND
        return if (provisionalLicenseHolder.isValidOn(date)) {
            DomainResponseStatus.OK
        } else {
            DomainResponseStatus.PROVISIONAL_LICENSE_NOT_VALID
        }
    }

    override suspend fun incrementProvisionalLicenseFailures(dossierId: String): DomainResponseStatus {
        val provisionalLicenseHolder = getProvisionalLicenseHolder(dossierId) ?: return DomainResponseStatus.ID_NOT_FOUND
        val holder = provisionalLicenseHolder.registerPracticalExamFailure()
        if (holder.hasMaxAttempts()) {
            val status = repositoryToDomainConversionTable.getDomainCode(repository.deleteProvisionalLicenseHolder(dossierId))
            if (status == DomainResponseStatus.OK) {
                return channelsProvider.dossierServiceChannel.updateExamStatus(dossierId, ExamEvent.PROVISIONAL_LICENSE_INVALIDATION)
            }
            return status
        }
        return repositoryToDomainConversionTable.getDomainCode(repository.updateProvisionalLicenseHolder(holder))
    }

    override suspend fun practicalExamSuccess(dossierId: String): DomainResponseStatus {
        val isDossierValidResult = channelsProvider.dossierServiceChannel.checkDossierValidity(dossierId)
        if (isDossierValidResult == DomainResponseStatus.OK) {
            val updateExamStateResult = channelsProvider.dossierServiceChannel.updateExamStatus(dossierId, ExamEvent.PRACTICAL_EXAM_PASSED)
            if (updateExamStateResult == DomainResponseStatus.OK) {
                return repositoryToDomainConversionTable.getDomainCode(repository.deleteProvisionalLicenseHolder(dossierId))
            }
            return updateExamStateResult
        }
        return isDossierValidResult
    }

    private suspend fun areThereAnotherProvisionalLicense(dossierId: String): Boolean {
        return repository.findProvisionalLicenseHolder(dossierId) != null
    }
}
/*******************************************************************************
 * MIT License
 *
 * Copyright 2023 DSDMS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 ******************************************************************************/

package dsdms.doctor.model.domainServices

import dsdms.doctor.channels.ChannelsProvider
import dsdms.doctor.database.Repository
import dsdms.doctor.handlers.getDomainCode
import dsdms.doctor.handlers.repositoryToDomainConversionTable
import dsdms.doctor.model.entities.DoctorDays
import dsdms.doctor.model.entities.DoctorSlot
import dsdms.doctor.model.entities.DoctorTimeSlot
import dsdms.doctor.model.valueObjects.DoctorApprovalEvent
import dsdms.doctor.model.valueObjects.DoctorResult
import dsdms.doctor.model.valueObjects.ResultTypes
import kotlinx.datetime.toLocalTime
import java.time.LocalDate

/**
 * Encapsulate domain operation response and payload if it is present.
 * @param domainResponseStatus response of operation
 * @param visitDate if there were no errors, null otherwise
 */
data class InsertDoctorVisitResult(
    val domainResponseStatus: DomainResponseStatus,
    val visitDate: String? = null,
)

/**
 * Encapsulate domain operation response and payload if it is present.
 * @param domainResponseStatus response of operation
 * @param doctorSlots list with doctor slots
 */
data class BookedDoctorSlots(
    val domainResponseStatus: DomainResponseStatus,
    val doctorSlots: List<DoctorSlot> = listOf(),
)

/**
 * @param repository for connection with storage
 * @param channelsProvider for connection with other domain contexts
 */
class DoctorDomainServiceImpl(
    private val repository: Repository,
    private val channelsProvider: ChannelsProvider,
) : DoctorDomainService {

    private suspend fun verifyDocuments(documents: DoctorSlot): DomainResponseStatus {
        return if (checkDoctorDay(documents.date)) {
            DomainResponseStatus.NOT_DOCTOR_DAY
        } else if (checkDoctorVisitGivenTime(documents.time)) {
            DomainResponseStatus.BAD_TIME
        } else if (checkTimeAvailability(documents.time, documents.date)) {
            DomainResponseStatus.TIME_OCCUPIED
        } else if (repository.getAllDoctorSlots(documents.dossierId, LocalDate.now()).isNotEmpty()) {
            DomainResponseStatus.DOSSIER_ALREADY_BOOKED
        } else {
            channelsProvider.dossierServiceChannel.checkDossierValidity(documents.dossierId)
        }
    }

    /**
     * @param date: the given date from doctor slot documents
     * @return if wanted date is a Doctor Day (Tuesday or Friday)
     */
    private fun checkDoctorDay(date: String): Boolean =
        DoctorDays.values().any { el -> el.name == LocalDate.parse(date).dayOfWeek.name }.not()

    /**
     * @param time: wanted time of the visit
     * @return if given time per the doctor visit is in the correct time slot and is available
     */
    private fun checkDoctorVisitGivenTime(time: String): Boolean =
        (
            time.toLocalTime() >= DoctorTimeSlot.InitTime.time &&
                time.toLocalTime() <= DoctorTimeSlot.FinishTime.time
            ).not()

    /**
     * @param time: wanted time of the visit
     * @param date: wanted date of the visit
     * @return if given time is available
     */
    private suspend fun checkTimeAvailability(time: String, date: String): Boolean =
        getOccupiedDoctorSlots(date).doctorSlots.any { el -> el.time == time }

    override suspend fun saveDoctorSlot(doctorSlot: DoctorSlot): InsertDoctorVisitResult {
        val verifyResult = verifyDocuments(doctorSlot)
        return if (verifyResult == DomainResponseStatus.OK) {
            InsertDoctorVisitResult(verifyResult, repository.saveDoctorSlot(doctorSlot))
        } else {
            InsertDoctorVisitResult(verifyResult)
        }
    }

    override suspend fun getOccupiedDoctorSlots(date: String): BookedDoctorSlots {
        val doctorSlots = repository.getOccupiedDoctorSlots(date)
        return if (doctorSlots.isEmpty()) {
            BookedDoctorSlots(DomainResponseStatus.NO_SLOT_OCCUPIED)
        } else {
            BookedDoctorSlots(
                DomainResponseStatus.OK,
                doctorSlots,
            )
        }
    }

    override suspend fun deleteDoctorSlot(dossierId: String): DomainResponseStatus {
        return repositoryToDomainConversionTable.getDomainCode(repository.deleteDoctorSlot(dossierId))
    }

    override suspend fun saveDoctorResult(doctorResult: DoctorResult): DomainResponseStatus {
        if (doctorResult.result != ResultTypes.VALID) {
            return DomainResponseStatus.EXAM_PASS_NOT_CREATED
        }
        val storingResult = repositoryToDomainConversionTable
            .getDomainCode(repository.registerDoctorResult(doctorResult))

        return if (storingResult == DomainResponseStatus.OK) {
            createTheoreticalExamPass(doctorResult)
        } else {
            storingResult
        }
    }

    private suspend fun createTheoreticalExamPass(document: DoctorResult): DomainResponseStatus {
        val doctorApprovalEvent = DoctorApprovalEvent(document.dossierId, document.date)
        return channelsProvider.examServiceChannel.notifyAboutDoctorApproval(doctorApprovalEvent)
    }
}

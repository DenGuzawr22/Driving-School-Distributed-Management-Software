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

package dsdms.exam.database

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import dsdms.exam.database.utils.RepositoryResponseStatus
import dsdms.exam.model.entities.ProvisionalLicense
import dsdms.exam.model.entities.theoreticalExam.TheoreticalExamAppeal
import dsdms.exam.model.entities.theoreticalExam.TheoreticalExamPass
import dsdms.exam.model.valueObjects.ProvisionalLicenseHolder
import kotlinx.datetime.LocalDate
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

/**
 * @param examService: represents the domain service used by repository.
 */
class RepositoryImpl(examService: CoroutineDatabase) : Repository {
    private val theoreticalExamPassDb =
        examService.getCollection<TheoreticalExamPass>("TheoreticalExamPass")
    private val theoreticalExamAppeals =
        examService.getCollection<TheoreticalExamAppeal>("TheoreticalExamAppeal")
    private val provisionalLicenseHolders =
        examService.getCollection<ProvisionalLicenseHolder>("ProvisionalLicenseHolders")

    override suspend fun dossierAlreadyHasOnePass(dossierId: String): Boolean {
        return theoreticalExamPassDb.find(TheoreticalExamPass::dossierId eq dossierId).toList().isNotEmpty()
    }

    override suspend fun saveNewTheoreticalExamPass(theoreticalExamPass: TheoreticalExamPass): TheoreticalExamPass {
        return theoreticalExamPass.apply { theoreticalExamPassDb.insertOne(theoreticalExamPass) }
    }

    override suspend fun getTheoreticalExamPass(dossierId: String): TheoreticalExamPass? {
        return theoreticalExamPassDb.findOne(TheoreticalExamPass::dossierId eq dossierId)
    }

    override suspend fun deleteTheoreticalExamPass(dossierId: String): RepositoryResponseStatus {
        return handleDeleteResult(theoreticalExamPassDb.deleteOne(TheoreticalExamPass::dossierId eq dossierId))
    }

    override suspend fun getFutureTheoreticalExamAppeals(): List<TheoreticalExamAppeal> {
        return theoreticalExamAppeals.find().toList().filter {
                el ->
            LocalDate.parse(el.date) > LocalDate.parse(java.time.LocalDate.now().toString())
        }
    }

    override suspend fun insertTheoreticalExamDay(newExamDay: TheoreticalExamAppeal): RepositoryResponseStatus {
        return handleInsertResult(theoreticalExamAppeals.insertOne(newExamDay))
    }

    override suspend fun updateExamAppeal(appealDate: String, appealList: List<String>): RepositoryResponseStatus {
        return handleUpdateResult(
            theoreticalExamAppeals
                .updateOne(
                    (TheoreticalExamAppeal::date eq appealDate),
                    setValue(TheoreticalExamAppeal::registeredDossiers, appealList),
                ),
        )
    }

    override suspend fun saveProvisionalLicenseHolder(
        provisionalLicenseHolder: ProvisionalLicenseHolder,
    ): RepositoryResponseStatus {
        return handleInsertResult(provisionalLicenseHolders.insertOne(provisionalLicenseHolder))
    }

    override suspend fun findProvisionalLicenseHolder(dossierId: String): ProvisionalLicenseHolder? {
        return provisionalLicenseHolders
            .findOne(ProvisionalLicenseHolder::provisionalLicense / ProvisionalLicense::dossierId eq dossierId)
    }

    override suspend fun deleteProvisionalLicenseHolder(dossierId: String): RepositoryResponseStatus {
        return handleDeleteResult(
            provisionalLicenseHolders
                .deleteOne(ProvisionalLicenseHolder::provisionalLicense / ProvisionalLicense::dossierId eq dossierId),
        )
    }

    override suspend fun updateProvisionalLicenseHolder(holder: ProvisionalLicenseHolder): RepositoryResponseStatus {
        return handleUpdateResult(
            provisionalLicenseHolders.updateOne(
                ProvisionalLicenseHolder::provisionalLicense / ProvisionalLicense::dossierId eq
                    holder.provisionalLicense.dossierId,
                holder,
            ),
        )
    }

    private fun handleUpdateResult(updateResult: UpdateResult): RepositoryResponseStatus {
        return if (updateResult.modifiedCount.toInt() != 1 || !updateResult.wasAcknowledged()) {
            RepositoryResponseStatus.UPDATE_ERROR
        } else {
            RepositoryResponseStatus.OK
        }
    }

    private fun handleInsertResult(insertOne: InsertOneResult): RepositoryResponseStatus {
        return if (insertOne.wasAcknowledged().not()) {
            RepositoryResponseStatus.INSERT_ERROR
        } else {
            RepositoryResponseStatus.OK
        }
    }

    private fun handleDeleteResult(deleteOne: DeleteResult): RepositoryResponseStatus {
        return if (deleteOne.wasAcknowledged().not()) {
            RepositoryResponseStatus.DELETE_ERROR
        } else if (deleteOne.deletedCount.toInt() == 0) {
            RepositoryResponseStatus.ID_NOT_FOUND
        } else {
            RepositoryResponseStatus.OK
        }
    }
}

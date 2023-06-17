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

package dsdms.exam.channels

import dsdms.exam.model.domainServices.DomainResponseStatus
import dsdms.exam.model.valueObjects.ExamEvent
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.await
import java.lang.IllegalStateException
import java.net.HttpURLConnection

/**
 * Allows to communicate with DossierContext.
 */
interface DossierServiceChannel {
    /**
     * Update dossier exam status.
     * @param dossierId
     * @param examEvent event about exam state change
     * @return DomainResponseStatus
     *  - OK
     *  - EXAM_STATUS_UPDATE_ERROR
     */
    suspend fun updateExamStatus(dossierId: String, examEvent: ExamEvent): DomainResponseStatus

    /**
     * Verify if dossier is valid or not.
     * @param dossierId
     * @return DomainResponseStatus
     * - OK
     * - DOSSIER_NOT_VALID
     * - DOSSIER_NOT_EXIST
     * @throws IllegalStateException if dossier context respond with not recognized status code
     */
    suspend fun checkDossierValidity(dossierId: String): DomainResponseStatus
}

/**
 * @param client -> vertx web client of dossier service.
 */
class DossierServiceChannelImpl(val client: WebClient) : DossierServiceChannel {

    override suspend fun updateExamStatus(dossierId: String, examEvent: ExamEvent): DomainResponseStatus {
        val result = client.put("/dossiers/$dossierId/examStatus")
            .sendBuffer(Buffer.buffer(examEvent.name)).await()
        if (result.statusCode() != HttpURLConnection.HTTP_OK) {
            println(result.body())
            return DomainResponseStatus.EXAM_STATUS_UPDATE_ERROR
        }
        return DomainResponseStatus.OK
    }

    override suspend fun checkDossierValidity(dossierId: String): DomainResponseStatus {
        val statusCode = client.get("/dossiers/$dossierId").send()
            .await()
            .statusCode()
        return when (statusCode) {
            HttpURLConnection.HTTP_OK -> DomainResponseStatus.OK
            HttpURLConnection.HTTP_ACCEPTED -> DomainResponseStatus.DOSSIER_NOT_VALID
            HttpURLConnection.HTTP_NOT_FOUND -> DomainResponseStatus.DOSSIER_NOT_EXIST
            else -> error("Can not get dossier information")
        }
    }
}

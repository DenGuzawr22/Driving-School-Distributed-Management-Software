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

package dsdms.client.cucumber.multiService

import dsdms.client.utils.SmartSleep
import dsdms.client.utils.VertxProviderImpl
import dsdms.client.utils.checkResponse
import dsdms.client.utils.createJson
import dsdms.dossier.model.entities.Dossier
import dsdms.dossier.model.valueObjects.ExamsStatus
import dsdms.exam.model.entities.ProvisionalLicense
import dsdms.exam.model.entities.theoreticalExam.TheoreticalExamAppeal
import dsdms.exam.model.valueObjects.ProvisionalLicenseHolder
import dsdms.exam.model.valueObjects.TheoreticalExamAppealUpdate
import io.cucumber.java8.En
import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.datetime.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.net.HttpURLConnection.HTTP_OK
import kotlin.test.assertEquals

/**
 * Provisional license creation test implementation.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/main/resources/features/multiService/provisionalLicenseRegistration.feature"],
    plugin = ["pretty", "summary"],
)
class ProvisionalLicenseCreation : En {
    private val dossierService: WebClient = VertxProviderImpl().getDossierServiceClient()
    private val examService: WebClient = VertxProviderImpl().getExamServiceClient()
    private val sleeper = SmartSleep()

    private var statusCode: Int? = null
    private var dossierId: String = "d99"
    private var examDate: LocalDate? = null
    private var provisionalLicenseHolderImpl: ProvisionalLicenseHolder? = null
    private var examStatus: ExamsStatus? = null

    init {
        Given("a new exam appeal on {word}") { date: String ->
            examDate = LocalDate.parse(date)
            val request = examService
                .post("/theoreticalExam/examAppeal")
                .sendBuffer(createJson(TheoreticalExamAppeal(date, 1)))
            val response = sleeper.waitResult(request)
            checkResponse(response)
            assertEquals(HTTP_OK, response?.statusCode())
        }
        When("the dossier is registered to exam appeal") {
            val request = examService
                .put("/theoreticalExam/examAppeal")
                .sendBuffer(createJson(TheoreticalExamAppealUpdate(dossierId, examDate!!.toString())))
            val response = sleeper.waitResult(request)
            checkResponse(response)
            assertEquals(HTTP_OK, response?.statusCode())
        }
        Then("the subscriber passes the theoretical exam so secretary creates new provisional license") {
            val response = createProvisionalLicense()
            assertEquals(HTTP_OK, response.statusCode())
        }
        And("if try to register an another provisional license for this dossier receive {word} msg") {
                exception: String ->
            val response = createProvisionalLicense()
            assertEquals(HTTP_CONFLICT, response.statusCode())
            assertEquals(exception, response.body().toString())
        }
        When("requesting info about registered provisional license") {
            println("Dossier $dossierId")
            val request = examService.get("/provisionalLicences/$dossierId").send()
            val response = sleeper.waitResult(request)
            checkResponse(response)
            println(response?.body())
            assertEquals(HTTP_OK, response?.statusCode())
            provisionalLicenseHolderImpl = Json.decodeFromString(response?.body().toString())
        }
        Then(
            "receiving info that there are {int} failing attempts and validity range is from {word} to {word}",
        ) { attempts: Int, startDate: String, endDate: String ->
            assertEquals(attempts, provisionalLicenseHolderImpl?.practicalExamAttempts)
            assertEquals(LocalDate.parse(startDate), provisionalLicenseHolderImpl?.provisionalLicense?.startValidity)
            assertEquals(LocalDate.parse(endDate), provisionalLicenseHolderImpl?.provisionalLicense?.endValidity)
        }
        When("secretary requests dossier exam status information") {
            val request = dossierService.get("/dossiers/$dossierId").send()
            val response = sleeper.waitResult(request)
            checkResponse(response)
            println(response?.body())
            assertEquals(HTTP_OK, response?.statusCode())
            val dossier: Dossier = Json.decodeFromString(response?.body().toString())
            examStatus = dossier.examsStatus
        }
        Then("theoretical exam state is {word} and practical exam state is {word}") {
                thResult: String, prResult: String ->
            assertEquals(thResult, examStatus!!.theoreticalExamState.name)
            assertEquals(prResult, examStatus!!.practicalExamState.name)
        }
    }

    private fun createProvisionalLicense(): HttpResponse<Buffer> {
        val request = examService
            .post("/provisionalLicences")
            .sendBuffer(createJson(ProvisionalLicense(dossierId, examDate!!)))
        val response = sleeper.waitResult(request)
        checkResponse(response)
        return response!!
    }
}

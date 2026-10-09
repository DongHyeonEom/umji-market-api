package com.buyeong.umji.api.account.integration.http

import com.buyeong.umji.api.enums.ErrorCode
import com.buyeong.umji.api.exception.ApiCallException
import com.buyeong.umji.api.exception.ClientBadRequestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class BusinessRegistrationStatusClientTest {
    @Test
    fun `upstream server failure maps to bad gateway error code`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(containsString("nts-businessman/v1/status")))
            .andRespond(withServerError())

        val exception = runCatching {
            BusinessRegistrationStatusClient(
                BusinessRegistrationStatusProperties("https://api.odcloud.kr/api", "test-key"),
                builder,
            ).lookup("1234567890")
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(ApiCallException::class.java)
        assertThat((exception as ApiCallException).errorCode).isEqualTo(ErrorCode.BAD_GATEWAY_ERROR)
        server.verify()
    }

    @Test
    fun `missing upstream service key maps to internal server error code`() {
        val exception = runCatching {
            BusinessRegistrationStatusClient(
                BusinessRegistrationStatusProperties(serviceKey = ""),
                RestClient.builder(),
            ).lookup("1234567890")
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(ApiCallException::class.java)
        assertThat((exception as ApiCallException).errorCode).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `status lookup allows ongoing and temporarily closed registrations`() {
        listOf("01", "02").forEach { statusCode ->
            val builder = RestClient.builder()
            val server = MockRestServiceServer.bindTo(builder).build()
            server.expect(requestTo(containsString("nts-businessman/v1/status")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(
                    withSuccess(
                        """{"status_code":"OK","data":[{"b_no":"1234567890","b_stt_cd":"$statusCode"}]}""",
                        MediaType.APPLICATION_JSON,
                    ),
                )

            BusinessRegistrationStatusClient(
                BusinessRegistrationStatusProperties("https://api.odcloud.kr/api", "test-key"),
                builder,
            ).ensureNotClosed("1234567890")
            server.verify()
        }
    }

    @Test
    fun `status lookup rejects closed registration`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(containsString("nts-businessman/v1/status")))
            .andRespond(
                withSuccess(
                    """{"status_code":"OK","data":[{"b_no":"1234567890","b_stt_cd":"03"}]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThatThrownBy {
            BusinessRegistrationStatusClient(
                BusinessRegistrationStatusProperties("https://api.odcloud.kr/api", "test-key"),
                builder,
            ).ensureNotClosed("1234567890")
        }.isInstanceOf(ClientBadRequestException::class.java)
        server.verify()
    }
}

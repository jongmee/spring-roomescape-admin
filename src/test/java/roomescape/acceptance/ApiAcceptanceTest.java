package roomescape.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import roomescape.dto.ReservationResponse;
import roomescape.dto.ReservationSaveRequest;
import roomescape.dto.ReservationTimeResponse;
import roomescape.dto.ReservationTimeSaveRequest;
import roomescape.exception.ErrorResponse;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static roomescape.TestFixture.*;

@Sql("/test-schema.sql")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ApiAcceptanceTest {

    @Test
    @DisplayName("[Step7] 예약 시간을 추가한다.")
    void createReservationTime() {
        // given & when
        ReservationTimeSaveRequest request = new ReservationTimeSaveRequest(MIA_RESERVATION_TIME);

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/times")
                .then().log().all()
                .extract();
        ReservationTimeResponse reservationTimeResponse = response.as(ReservationTimeResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusOk(response);
            assertThat(reservationTimeResponse.id()).isNotNull();
            assertThat(reservationTimeResponse.startAt()).isEqualTo(MIA_RESERVATION_TIME.toString());
        });
    }

    @Test
    @DisplayName("[Step7] 잘못된 형식의 예약 시간을 추가한다.")
    void createReservationTime2() {
        // given & when
        ReservationTimeSaveRequest request = new ReservationTimeSaveRequest(LocalTime.of(15, 3));

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/times")
                .then().log().all()
                .extract();
        ErrorResponse errorResponse = response.as(ErrorResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusBadRequest(response);
            assertThat(errorResponse.message()).isNotNull();
        });
    }

    @Test
    @DisplayName("[Step7] 예약 시간 목록을 조회한다.")
    void findReservationTimes() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/times")
                .then().log().all()
                .extract();
        List<ReservationTimeResponse> reservationTimeResponse = Arrays.stream(response.as(ReservationTimeResponse[].class))
                .toList();

        // then
        assertAll(() -> {
            checkHttpStatusOk(response);
            assertThat(reservationTimeResponse).hasSize(0);
        });
    }


    @TestFactory
    @DisplayName("[Step7] 예약 시간을 추가하고 삭제한다.")
    Stream<DynamicTest> createThenDeleteReservationTime() {
        return Stream.of(
                dynamicTest("예약을 하나 생성한다.", this::createReservationTime),
                dynamicTest("예약이 하나 생성된 예약 목록을 조회한다.", this::findReservationTimesWithSizeOne),
                dynamicTest("예약을 하나 삭제한다.", this::deleteOneReservationTime),
                dynamicTest("예약 목록을 조회한다.", this::findReservationTimes)
        );
    }

    @Test
    @DisplayName("[Step7] 존재하지 않는 예약 시간을 삭제한다.")
    void deleteNotExistingReservationTime() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().delete("/times/1")
                .then().log().all()
                .extract();
        ErrorResponse errorResponse = response.as(ErrorResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusNotFound(response);
            assertThat(errorResponse.message()).isNotNull();
        });
    }

    void findReservationTimesWithSizeOne() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/times")
                .then().log().all()
                .extract();
        List<ReservationTimeResponse> reservationTimeResponses = Arrays.stream(response.as(ReservationTimeResponse[].class))
                .toList();

        // then
        assertAll(() -> {
            checkHttpStatusOk(response);
            assertThat(reservationTimeResponses).hasSize(1)
                    .extracting(ReservationTimeResponse::id)
                    .contains(1L);
        });
    }

    void deleteOneReservationTime() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().delete("/times/1")
                .then().log().all()
                .extract();

        // then
        checkHttpStatusOk(response);
    }

    @Test
    @DisplayName("[Step2, Step5] 예약 목록을 조회한다.")
    void findReservations() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/reservations")
                .then().log().all()
                .extract();
        List<ReservationResponse> reservationResponses = Arrays.stream(response.as(ReservationResponse[].class))
                .toList();

        // then
        assertAll(() -> {
            checkHttpStatusOk(response);
            assertThat(reservationResponses).hasSize(0);
        });
    }

    @TestFactory
    @DisplayName("[Step3, Step6, Step8] 예약을 추가한다.")
    Stream<DynamicTest> createReservation() {
        return Stream.of(
                dynamicTest("예약 시간을 생성한다.", this::createReservationTime),
                dynamicTest("예약을 하나 생성한다.", this::createOneReservation)
        );
    }

    @Test
    @DisplayName("[Step3, Step6, Step8] 잘못된 형식의 예약을 추가한다.")
    void createInvalidReservation() {
        // given & when
        ReservationSaveRequest request = new ReservationSaveRequest(null, MIA_RESERVATION_DATE, 1L);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .extract();
        ErrorResponse errorResponse = response.as(ErrorResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusBadRequest(response);
            assertThat(errorResponse.message()).isNotNull();
        });
    }

    @Test
    @DisplayName("[Step3, Step6, Step8] 존재하지 않는 예약 시간에 예약을 추가한다.")
    void createReservationWithNotExistingTime() {
        // given & when
        ReservationSaveRequest request = new ReservationSaveRequest(USER_MIA, MIA_RESERVATION_DATE, 1L);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .extract();
        ErrorResponse errorResponse = response.as(ErrorResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusNotFound(response);
            assertThat(errorResponse.message()).isNotNull();
        });
    }

    @TestFactory
    @DisplayName("[Step3, Step6, Step8] 예약을 추가하고 삭제한다.")
    Stream<DynamicTest> createThenDeleteReservation() {
        return Stream.of(
                dynamicTest("예약 시간을 생성한다.", this::createReservationTime),
                dynamicTest("예약을 하나 생성한다.", this::createOneReservation),
                dynamicTest("예약이 하나 생성된 예약 목록을 조회한다.", this::findReservationsWithSizeOne),
                dynamicTest("예약을 하나 삭제한다.", this::deleteOneReservation),
                dynamicTest("예약 목록을 조회한다.", this::findReservations)
        );
    }

    @Test
    @DisplayName("[Step3, Step6, Step8] 존재하지 않는 예약을 삭제한다.")
    void deleteNotExistingReservation() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().delete("/reservations/1")
                .then().log().all()
                .extract();
        ErrorResponse errorResponse = response.as(ErrorResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusNotFound(response);
            assertThat(errorResponse.message()).isNotNull();
        });
    }

    void createOneReservation() {
        // given & when
        ReservationSaveRequest request = new ReservationSaveRequest(USER_MIA, MIA_RESERVATION_DATE, 1L);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .extract();
        ReservationResponse reservationResponse = response.as(ReservationResponse.class);

        // then
        assertAll(() -> {
            checkHttpStatusOk(response);
            assertThat(reservationResponse.id()).isNotNull();
            assertThat(reservationResponse.name()).isEqualTo(USER_MIA);
        });
    }

    void findReservationsWithSizeOne() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().get("/reservations")
                .then().log().all()
                .extract();
        List<ReservationResponse> reservationResponses = Arrays.stream(response.as(ReservationResponse[].class))
                .toList();

        // then
        assertAll(() -> {
            checkHttpStatusOk(response);
            assertThat(reservationResponses).hasSize(1)
                    .extracting(ReservationResponse::id)
                    .contains(1L);
        });
    }

    void deleteOneReservation() {
        // given & when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when().delete("/reservations/1")
                .then().log().all()
                .extract();

        // then
        checkHttpStatusOk(response);
    }

    void checkHttpStatusOk(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    void checkHttpStatusBadRequest(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    void checkHttpStatusNotFound(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}

package nextstep.subway.line;

import org.springframework.http.MediaType;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.line.dto.SectionRequest;

public class SectionAcceptanceMethod {
	public static ExtractableResponse<Response> addSection(String lineId, SectionRequest sectionRequest) {
		return RestAssured
			.given().log().all()
			.body(sectionRequest)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when()
			.post("/lines" + "/" + lineId + "/sections")
			.then().log().all()
			.extract();
	}

	public static ExtractableResponse<Response> removeStation(String lineId, Long stationId) {
		return RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when()
			.delete("/lines/" + lineId + "/sections?stationId=" + stationId)
			.then().log().all()
			.extract();
	}

	public static ExtractableResponse<Response> removeStation(Long stationId) {
		return RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when()
			.delete("/lines/sections?stationId=" + stationId)
			.then().log().all()
			.extract();
	}
}

package nextstep.subway.line;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.dto.LineRequest;

@DisplayName("지하철 노선 관련 기능")
public class LineAcceptanceTest extends AcceptanceTest {
	@Autowired
	LineAcceptanceMethod methods;

	@DisplayName("지하철 노선을 생성한다.")
	@Test
	void createLine() {
		// given
		// 지하철_노선_미등록_상태

		// when
		// 지하철_노선_생성_요청
		ExtractableResponse<Response> response = methods.createLine(new LineRequest("신분당선", "bg-red-600"));

		// then
		// 지하철_노선_생성됨
		assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(response.header("Location")).isNotBlank();
	}

	@DisplayName("기존에 존재하는 지하철 노선 이름으로 지하철 노선을 생성한다.")
	@Test
	void createLineWithDuplicateName() {
		// given
		// 지하철_노선_등록되어_있음
		methods.createLine(new LineRequest("신분당선", "bg-red-600"));

		// when
		// 지하철_노선_생성_요청
		ExtractableResponse<Response> response = methods.createLine(new LineRequest("신분당선", "bg-red-600"));

		// then
		// 지하철_노선_생성_실패됨
		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@DisplayName("지하철 노선 목록을 조회한다.")
	@Test
	void getLines() {
		// given
		// 지하철_노선_등록되어_있음
		ExtractableResponse<Response> createResponse1 = methods.createLine(
			new LineRequest("신분당선", "bg-red-600"));

		// 지하철_노선_등록되어_있음
		ExtractableResponse<Response> createResponse2 = methods.createLine(
			new LineRequest("2호선", "bg-green-600"));

		// when
		// 지하철_노선_목록_조회_요청
		ExtractableResponse<Response> response = methods.findAllLines();

		// then
		// 지하철_노선_목록_응답됨
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

		// 지하철_노선_목록_포함됨
		List<Long> expectedLineIds = Arrays.asList(createResponse1, createResponse2).stream()
			.map(it -> Long.parseLong(methods.getLineID(it)))
			.collect(Collectors.toList());

		List<Long> resultLineIds = response.jsonPath().getList(".", Line.class).stream()
			.map(it -> it.getId())
			.collect(Collectors.toList());

		assertThat(resultLineIds).containsAll(expectedLineIds);
	}

	@DisplayName("지하철 노선을 조회한다.")
	@Test
	void getLine() {
		// given
		// 지하철_노선_등록되어_있음
		ExtractableResponse<Response> createResponse = methods.createLine(
			new LineRequest("신분당선", "bg-red-600"));

		// when
		// 지하철_노선_조회_요청
		String id = methods.getLineID(createResponse);
		ExtractableResponse<Response> response = methods.findLine(id);

		// then
		// 지하철_노선_응답됨
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

		// 지하철_노선_동일함
		Line line = response.jsonPath().getObject(".", Line.class);
		assertThat(String.valueOf(line.getId())).isEqualTo(id);
	}

	@DisplayName("없는 지하철 노선을 조회한다.")
	@Test
	void getNotExistsLine() {
		// given
		// 지하철_노선_등록되어 있지 않음

		// when
		// 지하철_노선_조회_요청
		ExtractableResponse<Response> response = methods.findLine("1");

		// then
		// 지하철_노선_응답됨
		assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	@DisplayName("지하철 노선을 수정한다.")
	@Test
	void updateLine() {
		// given
		// 지하철_노선_등록되어_있음
		ExtractableResponse<Response> createResponse = methods.createLine(
			new LineRequest("신분당선", "bg-red-600"));

		// when
		// 지하철_노선_수정_요청
		ExtractableResponse<Response> modifyResponse = methods.updateLine(
			methods.getLineID(createResponse),
			new LineRequest("구분당선", "bg-red-600"));

		// then
		// 지하철_노선_수정_응답됨
		assertThat(modifyResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

		// 지하철_노선_수정됨
		ExtractableResponse<Response> response = methods.findLine(
			methods.getLineID(createResponse));
		Line line = response.jsonPath().getObject(".", Line.class);
		assertThat(line.getName()).isEqualTo("구분당선");
	}

	@DisplayName("없는 지하철 노선을 수정한다.")
	@Test
	void updateNotExistsLine() {
		// given
		// 지하철_노선_등록되어 있지 않음

		// when
		// 지하철_노선_조회_요청
		ExtractableResponse<Response> modifyResponse = methods.updateLine("1",
			new LineRequest("구분당선", "bg-red-600"));

		// then
		// 지하철_노선_응답됨
		assertThat(modifyResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	@DisplayName("지하철 노선을 제거한다.")
	@Test
	void deleteLine() {
		// given
		// 지하철_노선_등록되어_있음
		ExtractableResponse<Response> createResponse = methods.createLine(
			new LineRequest("신분당선", "bg-red-600"));

		// when
		// 지하철_노선_제거_요청
		ExtractableResponse<Response> deleteResponse = methods.deleteLine(
			methods.getLineID(createResponse));

		// then
		// 지하철_노선_삭제됨_응답
		assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

		// 지하철_노선_삭제됨
		ExtractableResponse<Response> response = methods.findLine(
			methods.getLineID(createResponse));
		assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	@DisplayName("없는 지하철 노선을 삭제한다.")
	@Test
	void deleteNotExistsLine() {
		// given
		// 지하철_노선_등록되어 있지 않음

		// when
		// 지하철_노선_조회_요청
		ExtractableResponse<Response> deleteResponse = methods.findLine("1");

		// then
		// 지하철_노선_응답됨
		assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}
}

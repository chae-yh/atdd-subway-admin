package nextstep.subway.line.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import nextstep.subway.station.domain.Station;

@Embeddable
public class Sections {
	@OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
	List<Section> sections = new ArrayList<>();

	public void add(Section candidate) {
		if (sections.size() == 0) {
			sections.add(candidate);

			return;
		}

		validateCandidate(candidate);

		if (addSectionWithCommonUpStation(candidate)) {
			return;
		}

		if (addSectionWithCommonDownStation(candidate)) {
			return;
		}

		sections.add(candidate);
	}

	private boolean addSectionWithCommonDownStation(Section candidate) {
		Section commonDownStationSection = sections.stream()
			.filter(x -> x.hasSameDownStation(candidate))
			.findFirst().orElse(null);

		if (commonDownStationSection != null) {
			rearrangeSections(commonDownStationSection, candidate, commonDownStationSection.getUpStation(),
				candidate.getUpStation());

			return true;
		}

		return false;
	}

	private boolean addSectionWithCommonUpStation(Section candidate) {
		Section commonUpStationSection = sections.stream()
			.filter(x -> x.hasSameUpStation(candidate))
			.findFirst().orElse(null);

		if (commonUpStationSection != null) {
			rearrangeSections(commonUpStationSection, candidate, candidate.getDownStation(),
				commonUpStationSection.getDownStation());

			return true;
		}

		return false;
	}

	private void rearrangeSections(Section targetSection, Section candidate, Station upStation, Station downStation) {
		if (targetSection.getDistance() - candidate.getDistance() <= 0) {
			throw new IllegalArgumentException("The distance between new section must be less than target section");
		}

		sections.add(
			new Section(targetSection.getLine(), upStation, downStation,
				targetSection.getDistance() - candidate.getDistance()));
		sections.add(candidate);
		sections.remove(targetSection);
	}

	private void validateCandidate(Section candidate) {
		Set<Station> stations = getDistinceStations();
		validateAlreadyExistsTwoStations(candidate, stations);
		validateExistsConnectedStationToOldLine(candidate, stations);
	}

	private Set<Station> getDistinceStations() {
		return sections.stream()
			.map(x -> Arrays.asList(x.getUpStation(), x.getDownStation()))
			.flatMap(y -> y.stream())
			.collect(Collectors.toSet());
	}

	private void validateExistsConnectedStationToOldLine(Section candidate, Set<Station> stations) {
		if (!stations.contains(candidate.getUpStation()) && !stations.contains(candidate.getDownStation())) {
			throw new NoSuchElementException("There is no such section");
		}
	}

	private void validateAlreadyExistsTwoStations(Section candidate, Set<Station> stations) {
		if (stations.contains(candidate.getUpStation()) && stations.contains(candidate.getDownStation())) {
			throw new IllegalArgumentException("Each two stations are already in the line");
		}
	}

	public List<Station> getOrderedStations() {
		return convertToOrderedList(getStationMap());
	}

	private Map<Station, Station> getStationMap() {
		return sections.stream().collect(Collectors.toMap(Section::getUpStation, Section::getDownStation));
	}

	private List<Station> convertToOrderedList(Map<Station, Station> order) {
		List<Station> stations = new ArrayList<>();
		Station key = getStartStation(order);

		stations.add(key);

		while (order.containsKey(key)) {
			stations.add(order.get(key));
			key = order.get(key);
		}

		return stations;
	}

	private Station getStartStation(Map<Station, Station> order) {
		return order.keySet()
			.stream()
			.filter(x -> !order.containsValue(x))
			.findFirst()
			.orElseThrow(() -> new NoSuchElementException("There is no start point"));
	}
}

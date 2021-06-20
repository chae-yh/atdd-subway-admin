package nextstep.subway.line.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import nextstep.subway.station.domain.Station;

@Embeddable
public class Sections {
	private static final int SIZE_ZERO = 0;
	private static final int MINIMUM_SECTION_COUNT = 1;

	@OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
	List<Section> sections = new ArrayList<>();

	public void add(Section candidate) {
		if (isSectionsEmpty()) {
			addSection(candidate);

			return;
		}

		validateCandidate(candidate);

		Optional<Section> commonUpStationSection = getCommonUpStationSection(candidate.getUpStation());
		if (commonUpStationSection.isPresent()) {
			addSectionWithCommonUpStation(candidate, commonUpStationSection.get());

			return;
		}

		Optional<Section> commonDownStationSection = getCommonDownStationSection(candidate.getDownStation());
		if (commonDownStationSection.isPresent()) {
			addSectionWithCommonDownStation(candidate, commonDownStationSection.get());

			return;
		}

		addSection(candidate);
	}

	private boolean isSectionsEmpty() {
		return sections.size() == SIZE_ZERO;
	}

	private void addSectionWithCommonDownStation(Section candidate, Section commonDownStationSection) {
		modifyOldSection(commonDownStationSection, candidate, commonDownStationSection.getUpStation(),
			candidate.getUpStation());
		addSection(candidate);
	}

	private Optional<Section> getCommonDownStationSection(Station downStation) {
		return sections.stream()
			.filter(section -> section.hasSameDownStation(downStation))
			.findFirst();
	}

	private void addSectionWithCommonUpStation(Section candidate, Section commonUpStationSection) {
		modifyOldSection(commonUpStationSection, candidate, candidate.getDownStation(),
			commonUpStationSection.getDownStation());
		addSection(candidate);
	}

	private void addSection(Section candidate) {
		sections.add(candidate);
	}

	private Optional<Section> getCommonUpStationSection(Station upStation) {
		return sections.stream()
			.filter(section -> section.hasSameUpStation(upStation))
			.findFirst();
	}

	private void modifyOldSection(Section targetSection, Section candidate, Station upStation, Station downStation) {
		if (targetSection.getDistance() - candidate.getDistance() <= Section.MINIMUM_DISTANCE) {
			throw new IllegalArgumentException("The distance between new section must be less than target section");
		}

		addSection(new Section(targetSection.getLine(), upStation, downStation,
			targetSection.getDistance() - candidate.getDistance()));
		sections.remove(targetSection);
	}

	private void validateCandidate(Section candidate) {
		Set<Station> stations = getDistinctStations();
		validateTwoStationsAlreadyExists(candidate, stations);
		validateConnectedStationToOldLineExists(candidate, stations);
	}

	private Set<Station> getDistinctStations() {
		return sections.stream()
			.map(section -> Arrays.asList(section.getUpStation(), section.getDownStation()))
			.flatMap(sections -> sections.stream())
			.collect(Collectors.toSet());
	}

	private void validateConnectedStationToOldLineExists(Section candidate, Set<Station> stations) {
		if (!stations.contains(candidate.getUpStation()) && !stations.contains(candidate.getDownStation())) {
			throw new NoSuchElementException("There is no such section");
		}
	}

	private void validateTwoStationsAlreadyExists(Section candidate, Set<Station> stations) {
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

	public void remove(Station targetStation) {
		validateRemovable();

		Optional<Section> sectionWithSameUpStation = getCommonUpStationSection(targetStation);
		Optional<Section> sectionWithSameDownStation = getCommonDownStationSection(targetStation);

		validateRequestStationExists(sectionWithSameUpStation, sectionWithSameDownStation);
		if (isUpAndDownStationExist(sectionWithSameUpStation, sectionWithSameDownStation)) {
			connectTwoStation(sectionWithSameUpStation.get(), sectionWithSameDownStation.get());
		}

		sectionWithSameUpStation.ifPresent(section -> sections.remove(section));
		sectionWithSameDownStation.ifPresent(section -> sections.remove(section));
	}

	private void validateRemovable() {
		if (sections.size() <= MINIMUM_SECTION_COUNT) {
			throw new IllegalArgumentException("Section must be over at least two count in line");
		}
	}

	private void connectTwoStation(Section sectionWithSameUpStation,
		Section sectionWithSameDownStation) {
		sections.add(new Section(sectionWithSameUpStation.getLine(),
			sectionWithSameDownStation.getUpStation(),
			sectionWithSameUpStation.getDownStation(),
			sectionWithSameUpStation.getDistance() + sectionWithSameDownStation.getDistance()));
	}

	private boolean isUpAndDownStationExist(Optional<Section> sectionWithSameUpStation,
		Optional<Section> sectionWithSameDownStation) {
		return sectionWithSameUpStation.isPresent() && sectionWithSameDownStation.isPresent();
	}

	private void validateRequestStationExists(Optional<Section> sectionWithSameUpStation,
		Optional<Section> sectionWithSameDownStation) {
		if (!sectionWithSameUpStation.isPresent() && !sectionWithSameDownStation.isPresent()) {
			throw new NoSuchElementException("Request station is not on the line");
		}
	}
}

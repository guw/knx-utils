package io.guw.knxutils.semanticanalyzer;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.guw.knxutils.knxprojectparser.GroupAddress;
import io.guw.knxutils.knxprojectparser.KnxProjectFile;

public class KnxProjectAnalyzer {

	private static final Logger LOG = LoggerFactory.getLogger(KnxProjectAnalyzer.class);

	private final KnxProjectFile knxProjectFile;
	private final KnxProjectCharacteristics characteristics;
	private final List<Light> lights = new ArrayList<>();

	public KnxProjectAnalyzer(KnxProjectFile knxProjectFile, KnxProjectCharacteristics characteristics) {
		this.knxProjectFile = knxProjectFile;
		this.characteristics = characteristics;
	}

	public void analyze() {
		List<GroupAddress> groupAddresses = knxProjectFile.getGroupAddresses();
		if (groupAddresses.isEmpty()) {
			throw new IllegalStateException("The project does not contain any Group Address.");
		}

		// fill in missing information
		groupAddresses.forEach(characteristics::fillInMissingInformation);

		// sanity check
		if (((float) characteristics.getWarnings() / (float) groupAddresses.size()) > 0.10F) {
			LOG.warn("The project data generated a lot of warnings. Please consider improving the ETS data.");
		}

		// index all GAs
		characteristics.learn(groupAddresses);

		// find lights
		List<GroupAddress> lightGroupAddresses = groupAddresses.parallelStream().filter(characteristics::isLight)
				.collect(toList());

		// group light GAs based on primaries
		List<GroupAddress> primaryLightGroupAddresses = lightGroupAddresses.parallelStream()
				.filter(characteristics::isPrimarySwitch).collect(toList());

		// build potential lights
		primaryLightGroupAddresses.forEach(this::analyzeLight);

		// find shutters

	}

	private void analyzeLight(GroupAddress ga) {
		GroupAddress statusGa = characteristics.findMatchingStatusGroupAddress(ga);
		if (statusGa == null) {
			LOG.debug("Unable to find matching status GA for GA {} ({})", ga, ga.getName());
			return;
		}

		GroupAddress dimGa = characteristics.findMatchingDimGroupAddress(ga);
		GroupAddress brightnessGa = characteristics.findMatchingBrightnessGroupAddress(ga);
		GroupAddress brightnessStatusGa = characteristics.findMatchingBrightnessStatusGroupAddress(ga);
		if ((dimGa != null) && (brightnessGa != null) && (brightnessStatusGa != null)) {
			// use dimmable light
			String name = characteristics.findName(ga, statusGa, dimGa, brightnessGa, brightnessStatusGa);
			lights.add(new DimmableLight(name, ga, statusGa, dimGa, brightnessGa, brightnessStatusGa));
		} else {
			// go with simple light
			String name = characteristics.findName(ga, statusGa);
			lights.add(new Light(name, ga, statusGa));
		}
	}

	public KnxProjectCharacteristics getCharacteristics() {
		return characteristics;
	}

	public KnxProjectFile getKnxProjectFile() {
		return knxProjectFile;
	}

	public List<Light> getLights() {
		return lights;
	}
}

package io.guw.knxopenhabutils.semanticanalyzer;

import static java.util.stream.Collectors.toList;

import java.util.List;

import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;
import io.guw.knxopenhabutils.knxprojectparser.KnxProjectFile;

public class KnxProjectAnalyzer {

	private final KnxProjectFile knxProjectFile;
	private final KnxProjectCharacteristics characteristics;

	public KnxProjectAnalyzer(KnxProjectFile knxProjectFile, KnxProjectCharacteristics characteristics) {
		this.knxProjectFile = knxProjectFile;
		this.characteristics = characteristics;
	}

	public void analyze() {
		// index all GAs
		characteristics.learn(knxProjectFile.getGroupAddresses());

		// find lights
		List<GroupAddress> lightGroupAddresses = knxProjectFile.getGroupAddresses().parallelStream()
				.filter(characteristics::isLight).collect(toList());

		// get all
		for (GroupAddress groupAddress : lightGroupAddresses) {
			System.out.println("Light: " + groupAddress);
		}

		// find shutters
	}

	public KnxProjectCharacteristics getCharacteristics() {
		return characteristics;
	}

	public KnxProjectFile getKnxProjectFile() {
		return knxProjectFile;
	}
}

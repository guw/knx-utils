package io.guw.knxopenhabutils.semanticanalyzer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;

public class GenericGermanyKnxProjectCharacteristicsTest {

	private GenericGermanyKnxProjectCharacteristics characteristics;

	private GroupAddress gaWithName(String name) {
		GroupAddress groupAddress = new GroupAddress(null, null, 0, name, null, null);
		characteristics.learn(List.of(groupAddress));
		return groupAddress;
	}

	@Test
	public void isLight() throws Exception {
		assertTrue(characteristics.isLight(gaWithName("Licht")));
		assertTrue(characteristics.isLight(gaWithName("LiChT")));
		assertTrue(characteristics.isLight(gaWithName("LICHT")));
		assertTrue(characteristics.isLight(gaWithName("licht")));

		assertTrue(characteristics.isLight(gaWithName("Lichter")));

		assertTrue(characteristics.isLight(gaWithName("Spiegellicht")));

		assertTrue(characteristics.isLight(gaWithName("Leuchte")));
	}

	@BeforeEach
	public void setup() {
		characteristics = new GenericGermanyKnxProjectCharacteristics();
	}

	@AfterEach
	public void tearDown() {

	}

}

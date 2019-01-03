package io.guw.knxopenhabutils.semanticanalyzer;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.guw.knxopenhabutils.knxprojectparser.DatapointType;
import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;

public class GenericGermanyKnxProjectCharacteristicsTest {

	private GenericGermanyKnxProjectCharacteristics characteristics;

	private void assertSingleTerm(String text, String expected) throws Exception {
		Set<String> terms = characteristics.getTerms(text);
		assertEquals(1, terms.size(), () -> format("expected terms of size one for input: '%s' but go: '%s'", text,
				terms.stream().collect(joining(" "))));
		assertEquals(expected, terms.stream().findFirst().get());
	}

	private void assertTerms(String text, String... expectedTerms) throws Exception {
		Set<String> terms = characteristics.getTerms(text);
		assertEquals(expectedTerms.length, terms.size(),
				() -> format("expected terms of size %d for input: '%s' but go: '%s'", expectedTerms.length, text,
						terms.stream().collect(joining(" "))));
		Arrays.stream(expectedTerms)
				.forEach((term) -> assertTrue(terms.contains(term),
						() -> format("expected term '%s' for input: '%s' but got: '%s'", term, text,
								terms.stream().collect(joining(" ")))));
	}

	private GroupAddress gaWithName(String name) {
		return gaWithNameAndDpt(name, DatapointType.Switch.getValue());
	}

	private GroupAddress gaWithNameAndDpt(String name, String dpt) {
		GroupAddress groupAddress = new GroupAddress(null, null, 0, name, null, dpt);
		characteristics.learn(List.of(groupAddress));
		return groupAddress;
	}

	@Test
	public void getTerms() throws Exception {
		assertSingleTerm("Licht", "licht");
		assertSingleTerm("Lichter", "licht");

		assertSingleTerm("Leuchte", "leucht");

		assertSingleTerm("Status", "status");

		assertTrue(characteristics.getTerms("Spiegellicht").contains("licht"));
		assertTerms("Spiegellicht", "spiegel", "licht", "spiegellicht");

		assertTerms("Licht Küche Status Ein/Aus", "status", "licht", "kuch");
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

	@Test
	public void isPrimarySwitch() throws Exception {
		assertTrue(characteristics.isPrimarySwitch(gaWithName("Licht Küche Ein/Aus")));
		assertFalse(characteristics.isPrimarySwitch(gaWithName("Licht Küche Status Ein/Aus")));
	}

	@BeforeEach
	public void setup() {
		characteristics = new GenericGermanyKnxProjectCharacteristics();
	}

	@AfterEach
	public void tearDown() {

	}

}

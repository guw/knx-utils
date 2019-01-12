package io.guw.knxopenhabutils.semanticanalyzer;

import static io.guw.knxopenhabutils.knxprojectparser.DatapointType.State;
import static io.guw.knxopenhabutils.knxprojectparser.DatapointType.Switch;
import static io.guw.knxopenhabutils.knxprojectparser.GroupAddress.getCombindedAddress;
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

import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;
import io.guw.knxopenhabutils.knxprojectparser.GroupAddressRange;

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

	@Test
	public void calculatePrefixMatchRatio() throws Exception {
		assertTrue(
				0.6F <= characteristics.calculatePrefixMatchRatio("Licht Küche Status Ein/Aus", "Licht Küche Ein/Aus"));
	}

	@Test
	public void findMatchingStatusGroupAddress_pattern_block() throws Exception {
		GroupAddress primary = ga(1, 0, 1, "Licht Küche Ein/Aus", Switch.getValue());
		GroupAddress status = ga(1, 0, 4, "Licht Küche Status", State.getValue());
		assertEquals(status, characteristics.findMatchingStatusGroupAddress(primary));
	}

	@Test
	public void findMatchingStatusGroupAddress_pattern_group() throws Exception {
		GroupAddressRange lightsRange = new GroupAddressRange(null, null, 2048, 4095, "Lichter", null);
		GroupAddressRange statusRange = new GroupAddressRange(null, null, 20480, 22527, "Rückmelde-/Statusobjekte",
				null);

		GroupAddress primary = ga(lightsRange, 1, 0, 1, "Licht Küche Ein/Aus", Switch.getValue());
		GroupAddress status = ga(statusRange, 10, 0, 1, "Licht Küche Status", State.getValue());
		assertEquals(status, characteristics.findMatchingStatusGroupAddress(primary));
	}

	private GroupAddress ga(GroupAddressRange range, int part1, int part2, int part3, String name, String dpt) {
		GroupAddress groupAddress = new GroupAddress(range, null, getCombindedAddress(part1, part2, part3), name, null,
				dpt);
		characteristics.learn(List.of(groupAddress));
		return groupAddress;
	}

	private GroupAddress ga(int part1, int part2, int part3, String name, String dpt) {
		return ga(null, part1, part2, part3, name, dpt);
	}

	private GroupAddress gaWithName(String name) {
		return gaWithNameAndDpt(name, Switch.getValue());
	}

	private GroupAddress gaWithNameAndDpt(String name, String dpt) {
		return ga(1, 0, 1, name, dpt);
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

		assertTerms("Rückmeldungen Schalten", "schalt", "ruckmeldung", "ruck");

		assertTerms("Rückmelde-/Statusobjekte", "ruckmeld", "ruck", "statusobjekt", "objekt", "status", "meld");
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
	public void isMatchingStatus() throws Exception {
		assertTrue(
				characteristics.isMatchingStatus(gaWithName("Licht Küche Status"), gaWithName("Licht Küche Ein/Aus")));
	}

	@Test
	public void isPrimarySwitch() throws Exception {
		assertTrue(characteristics.isPrimarySwitch(gaWithName("Licht Küche Ein/Aus")));
		assertFalse(characteristics.isPrimarySwitch(gaWithName("Licht Küche Status Ein/Aus")));
		assertFalse(characteristics.isPrimarySwitch(gaWithName("Rückmeldungen Schalten Licht")));
	}

	@BeforeEach
	public void setup() {
		characteristics = new GenericGermanyKnxProjectCharacteristics();
	}

	@AfterEach
	public void tearDown() {

	}

}

package io.guw.knxopenhabutils.knxprojectparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class GroupAddressTest {

	@Test
	public void formatAsThreePartAddress() throws Exception {
		assertEquals("1/0/10", GroupAddress.formatAsThreePartAddress(2058));
		assertEquals("1/0/0", GroupAddress.formatAsThreePartAddress(2048));
	}

}

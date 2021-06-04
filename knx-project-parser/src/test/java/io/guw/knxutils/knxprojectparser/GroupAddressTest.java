package io.guw.knxutils.knxprojectparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.guw.knxutils.knxprojectparser.GroupAddress;

public class GroupAddressTest {

	@Test
	public void formatAsThreePartAddress() throws Exception {
		assertEquals("1/0/10", GroupAddress.formatAsThreePartAddress(2058));
		assertEquals("1/0/0", GroupAddress.formatAsThreePartAddress(2048));
		assertEquals("0/0/0", GroupAddress.formatAsThreePartAddress(0));
	}

	@Test
	public void getCombindedAddress() throws Exception {
		assertEquals(2058, GroupAddress.getCombindedAddress(1, 0, 10));
		assertEquals(2048, GroupAddress.getCombindedAddress(1, 0, 0));
		assertEquals(0, GroupAddress.getCombindedAddress(0, 0, 0));
	}

}

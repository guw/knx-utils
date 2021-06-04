package io.guw.knxutils.knxprojectparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.guw.knxutils.knxprojectparser.Area;
import io.guw.knxutils.knxprojectparser.Device;
import io.guw.knxutils.knxprojectparser.Line;

public class DeviceTest {

	@Test
	public void formatAsPhysicalAddress() throws Exception {
		assertNull(Device.formatAsPhysicalAddress(null, null));
		assertNull(Device.formatAsPhysicalAddress(new Line(null, null, null, null, null), null));
		assertNull(Device.formatAsPhysicalAddress(new Line(null, null, null, null, null), "1"));
		assertNull(Device.formatAsPhysicalAddress(new Line(null, null, "2", null, null), "1"));
		assertNull(
				Device.formatAsPhysicalAddress(new Line(new Area(null, null, null, null), null, "2", null, null), "1"));
		assertEquals("3.2.1",
				Device.formatAsPhysicalAddress(new Line(new Area(null, "3", null, null), null, "2", null, null), "1"));
	}

}

package io.guw.knxopenhabutils.knxprojectparser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An attempt to define constants for most common and relevant DPT.
 */
public enum DatapointType {

	// @formatter:off
	Switch("1.001"),
	Bool("1.002"),
	Enable("1.003"),

	UpDown("1.008"),
	OpenClose("1.009"),

	State("1.011"),

	ControlDimming("3.007"),

	Scaling("5.001"),

	// @formatter:on
	;

	private static final Map<String, DatapointType> dptByValue;
	static {
		dptByValue = buildDptByValueCache();
	}

	private static final Map<String, DatapointType> buildDptByValueCache() {
		Map<String, DatapointType> dptByValueCache = new HashMap<>();
		for (DatapointType dpt : DatapointType.values()) {
			dptByValueCache.put(dpt.getValue(), dpt);
		}
		return Collections.unmodifiableMap(dptByValueCache);
	}

	public static DatapointType findByKnxProjectValue(String value) {
		return dptByValue.get(value);
	}

	private final String value;

	private DatapointType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}

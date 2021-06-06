package io.guw.knxutils.semanticanalyzer;

import io.guw.knxutils.knxprojectparser.GroupAddress;

public class Light {

	private final GroupAddress primarySwitchGroupAddress;
	private final GroupAddress statusGroupAddress;
	private final String name;

	public Light(String name, GroupAddress primarySwitchGroupAddress, GroupAddress statusGroupAddress) {
		this.name = name;
		this.primarySwitchGroupAddress = primarySwitchGroupAddress;
		this.statusGroupAddress = statusGroupAddress;
	}

	public String getName() {
		return name;
	}

	public GroupAddress getPrimarySwitchGroupAddress() {
		return primarySwitchGroupAddress;
	}

	public GroupAddress getStatusGroupAddress() {
		return statusGroupAddress;
	}

	@Override
	public String toString() {
		return "Light (" + primarySwitchGroupAddress + ", status: " + statusGroupAddress + ")";
	}
}

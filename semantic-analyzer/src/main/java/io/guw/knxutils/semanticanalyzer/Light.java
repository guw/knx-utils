package io.guw.knxutils.semanticanalyzer;

import io.guw.knxutils.knxprojectparser.GroupAddress;

public class Light {

	private final GroupAddress primarySwitchGroupAddress;
	private final GroupAddress statusGroupAddress;

	public Light(GroupAddress primarySwitchGroupAddress, GroupAddress statusGroupAddress) {
		this.primarySwitchGroupAddress = primarySwitchGroupAddress;
		this.statusGroupAddress = statusGroupAddress;
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

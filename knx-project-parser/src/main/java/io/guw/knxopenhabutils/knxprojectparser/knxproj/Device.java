package io.guw.knxopenhabutils.knxprojectparser.knxproj;

import java.util.ArrayList;
import java.util.List;

public class Device {

	static String formatAsPhysicalAddress(Line line, String address) {
		if ((address == null) || (line == null)) {
			return address;
		}

		return line.getArea().getAddress() + "." + line.getAddress() + "." + address;
	}

	private final Line line;
	private final String id;
	private final String address;
	private final String name;
	private final String description;
	private final List<CommunicationObject> communicationObjects = new ArrayList<>();

	public Device(Line line, String id, String address, String name, String description) {
		this.line = line;
		this.id = id;
		this.address = formatAsPhysicalAddress(line, address);
		this.name = name;
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public List<CommunicationObject> getCommunicationObjects() {
		return communicationObjects;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public Line getLine() {
		return line;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Device [address=" + address + ", id=" + id + "]";
	}

}

package io.guw.knxopenhabutils.knxprojectparser.knxproj;

import java.util.ArrayList;
import java.util.List;

public class GroupAddress {

	static String formatAsThreePartAddress(int address) {
		int part1 = address >> 11;
		int part2 = (address >> 8) & 7;
		int part3 = address & 0xFF;

		return part1 + "/" + part2 + "/" + part3;
	}

	private final GroupAddressRange groupAddressRange;
	private final String id;
	private final String address;
	private final String name;
	private final String description;
	private final List<CommunicationObject> writingCommunicationObjects = new ArrayList<>();
	private final List<CommunicationObject> listeningCommunicationObjects = new ArrayList<>();
	private final String datapointType;

	public GroupAddress(GroupAddressRange groupAddressRange, String id, int address, String name, String description,
			String datapointType) {
		this.groupAddressRange = groupAddressRange;
		this.id = id;
		this.address = formatAsThreePartAddress(address);
		this.name = name;
		this.description = description;
		this.datapointType = datapointType;
	}

	public String getAddress() {
		return address;
	}

	public String getDatapointType() {
		return datapointType;
	}

	public String getDescription() {
		return description;
	}

	public GroupAddressRange getGroupAddressRange() {
		return groupAddressRange;
	}

	public String getId() {
		return id;
	}

	public List<CommunicationObject> getListeningCommunicationObjects() {
		return listeningCommunicationObjects;
	}

	public String getName() {
		return name;
	}

	public List<CommunicationObject> getWritingCommunicationObjects() {
		return writingCommunicationObjects;
	}

	@Override
	public String toString() {
		return address + " [" + name + "]";
	}

}

package io.guw.knxutils.knxprojectparser;

import java.util.ArrayList;
import java.util.List;

public class GroupAddress {

	public static String formatAsThreePartAddress(int address) {
		return getAddressPart1(address) + "/" + getAddressPart2(address) + "/" + getAddressPart3(address);
	}

	public static String formatAsThreePartAddress(int part1, int part2, int part3) {
		return part1 + "/" + part2 + "/" + part3;
	}

	public static int getAddressPart1(int address) {
		return (address & 0x7800) >> 11;
	}

	public static int getAddressPart2(int address) {
		return (address & 0x700) >> 8;
	}

	public static int getAddressPart3(int address) {
		return address & 0xFF;
	}

	public static int getCombindedAddress(int part1, int part2, int part3) {
		return (part1 << 11) + (part2 << 8) + part3;
	}

	private final GroupAddressRange groupAddressRange;
	private final String id;
	private final String address;
	private final List<CommunicationObject> writingCommunicationObjects = new ArrayList<>();

	private final List<CommunicationObject> listeningCommunicationObjects = new ArrayList<>();
	private String name;
	private String description;
	private String datapointType;
	private final int addressInt;

	public GroupAddress(GroupAddressRange groupAddressRange, String id, int address, String name, String description,
			String datapointType) {
		this.groupAddressRange = groupAddressRange;
		this.id = id;
		addressInt = address;
		this.address = formatAsThreePartAddress(address);
		this.name = name;
		this.description = description;
		this.datapointType = datapointType;
	}

	public String getAddress() {
		return address;
	}

	public int getAddressInt() {
		return addressInt;
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

	public void setDatapointType(String datapointType) {
		this.datapointType = datapointType;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return address + " [" + name + ", dpt=" + datapointType + "]";
	}

}

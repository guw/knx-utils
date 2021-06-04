package io.guw.knxutils.knxprojectparser;

import java.util.ArrayList;
import java.util.List;

public class CommunicationObject {

	private final Device device;
	private final String refId;
	private final String datapointType;
	private final String description;
	private String sendGroupAddressRefId;
	private final List<String> listenGroupAddressRefIds = new ArrayList<>();
	private GroupAddress sendGroupAddress;
	private final List<GroupAddress> listenGroupAddresses = new ArrayList<>();

	private final boolean readFlag;

	public CommunicationObject(Device device, String refId, String datapointType, String description,
			boolean readFlag) {
		this.device = device;
		this.refId = refId;
		this.datapointType = datapointType;
		this.description = description;
		this.readFlag = readFlag;
	}

	public String getDatapointType() {
		return datapointType;
	}

	public String getDescription() {
		return description;
	}

	public Device getDevice() {
		return device;
	}

	public List<GroupAddress> getListenGroupAddresses() {
		return listenGroupAddresses;
	}

	public List<String> getListenGroupAddressRefIds() {
		return listenGroupAddressRefIds;
	}

	public String getRefId() {
		return refId;
	}

	public GroupAddress getSendGroupAddress() {
		return sendGroupAddress;
	}

	public String getSendGroupAddressRefId() {
		return sendGroupAddressRefId;
	}

	public boolean isReadFlag() {
		return readFlag;
	}

	public void setSendGroupAddress(GroupAddress sendGroupAddress) {
		this.sendGroupAddress = sendGroupAddress;
	}

	public void setSendGroupAddressRefId(String sendGroupAddressRefId) {
		this.sendGroupAddressRefId = sendGroupAddressRefId;
	}

	@Override
	public String toString() {
		return "CommunicationObject ["
				+ ((description != null) && !description.isBlank() ? description : "<missing description>") + ", dpt "
				+ datapointType + (readFlag ? ", READ" : "") + ", " + device + "]";
	}
}

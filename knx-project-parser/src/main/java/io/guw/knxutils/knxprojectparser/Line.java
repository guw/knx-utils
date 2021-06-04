package io.guw.knxutils.knxprojectparser;

import java.util.ArrayList;
import java.util.List;

public class Line {

	private final Area area;
	private final String id;
	private final String address;
	private final String name;
	private final String description;
	private final List<Device> devices = new ArrayList<>();

	public Line(Area area, String id, String address, String name, String description) {
		this.area = area;
		this.id = id;
		this.address = address;
		this.name = name;
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public Area getArea() {
		return area;
	}

	public String getDescription() {
		return description;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Line [id=" + id + ", address=" + address + "]";
	}

}

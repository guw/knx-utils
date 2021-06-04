package io.guw.knxutils.knxprojectparser;

import java.util.ArrayList;
import java.util.List;

public class Area {

	private final String id;
	private final String address;
	private final String name;
	private final String description;
	private final List<Line> lines = new ArrayList<>();

	public Area(String id, String address, String name, String description) {
		this.id = id;
		this.address = address;
		this.name = name;
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public List<Line> getLines() {
		return lines;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Area [id=" + id + ", address=" + address + "]";
	}

}

package io.guw.knxopenhabutils.knxprojectparser.knxproj;

public class GroupAddressRange {

	private final String id;
	private final String start;
	private final String end;
	private final String name;
	private final String description;
	private final GroupAddressRange parent;

	public GroupAddressRange(GroupAddressRange parent, String id, String start, String end, String name,
			String description) {
		this.parent = parent;
		this.id = id;
		this.start = start;
		this.end = end;
		this.name = name;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getEnd() {
		return end;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public GroupAddressRange getParent() {
		return parent;
	}

	public String getStart() {
		return start;
	}

	@Override
	public String toString() {
		return "GroupAddressRange [id=" + id + ", start=" + start + ", name=" + name + "]";
	}

}

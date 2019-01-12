package io.guw.knxopenhabutils.knxprojectparser;

public class GroupAddressRange {

	private final String id;
	private final String start;
	private final String end;
	private final String name;
	private final String description;
	private final GroupAddressRange parent;
	private final int startInt;
	private final int endInt;

	public GroupAddressRange(GroupAddressRange parent, String id, int start, int end, String name, String description) {
		this.parent = parent;
		this.id = id;
		this.start = GroupAddress.formatAsThreePartAddress(start);
		startInt = start;
		this.end = GroupAddress.formatAsThreePartAddress(end);
		endInt = end;
		this.name = name;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getEnd() {
		return end;
	}

	public int getEndInt() {
		return endInt;
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

	public int getStartInt() {
		return startInt;
	}

	@Override
	public String toString() {
		return "GroupAddressRange [id=" + id + ", start=" + start + ", name=" + name + "]";
	}
}

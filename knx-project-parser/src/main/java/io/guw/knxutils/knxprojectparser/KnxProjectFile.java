package io.guw.knxutils.knxprojectparser;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple reader/parser for <code>.knxproj</code> files exported from ETS.
 * <p>
 * After creating a {@link KnxProjectFile} instance it must be {@link #open()
 * opened}. This will populate the object with data from the
 * <code>.knxproj</code> file.
 * </p>
 * <p>
 * A few limitations apply:
 * <ul>
 * <li>must be exported from a most recent ETS version</li>
 * <li>only one project supported, i.e. only one project must be exported into
 * the <code>.knxproj</code> file</li>
 * </ul>
 * </p>
 */
public class KnxProjectFile {

	interface ChildElementHandler {
		void onChildElement(String elementName) throws XMLStreamException;
	}

	private static final Logger LOG = LoggerFactory.getLogger(KnxProjectFile.class);

	static String convertToDpt(XMLStreamReader streamReader, String datapointType) {
		if ((datapointType == null) || datapointType.isBlank()) {
			return null;
		}

		if (datapointType.contains(" ")) {
			LOG.warn("Found invalid DPT '{}' at {}. Dropping everything following including first space.",
					datapointType, streamReader.getLocation());
			datapointType = datapointType.split(" ")[0];
		}

		if (datapointType.startsWith("DPST") || datapointType.startsWith("DPT")) {
			var type1 = 0;
			var type2 = 0;
			String[] parts = datapointType.split("-");
			if (parts[0].equals("DPST")) {
				type1 = Integer.parseInt(parts[1]);
				type2 = Integer.parseInt(parts[2]);
			} else if (parts[0].equals("DPT")) {
				type1 = Integer.parseInt(parts[1]);
				type2 = 0;
			}
			return format("%d.%03d", type1, type2);
		}

		switch (datapointType) {
		case "1 Bit":
			return "1.001";
		case "2 Bit":
			return "2.001";
		default:
			return "0.000";
		}
	}

	static String eventTypeName(int eventType) {
		switch (eventType) {
		case START_ELEMENT:
			return "start tag";

		case END_ELEMENT:
			return "end element";

		default:
			return String.valueOf(eventType);
		}
	}

	static void forwardTo(int eventType, XMLStreamReader streamReader, String elementName) throws XMLStreamException {
		if ((eventType != START_ELEMENT) && (eventType != END_ELEMENT)) {
			throw new IllegalArgumentException("only START_ELEMENT or END_ELEMENT supported");
		}
		while (streamReader.hasNext()) {
			if (streamReader.getEventType() == eventType) {
				if (elementName.equals(streamReader.getLocalName())) {
					return;
				}
			}
			streamReader.next();
		}
		throw new XMLStreamException(format("Did not find %s for element '%s'", eventTypeName(eventType), elementName));
	}

	private int readElementChildrenLevel;

	private final File file;

	private String projectId;
	private String projectName;
	private final Map<String, Device> devicesById = new HashMap<>();

	private final Map<String, GroupAddress> groupAddressById = new HashMap<>();

	/**
	 * Creates a new project file for the specified file.
	 *
	 * @param knxProjFile the <code>.knxproj</code> file to read
	 */
	public KnxProjectFile(File knxProjFile) {
		file = knxProjFile;
	}

	/**
	 * @return a list of devices read from the KNX project (modifications to the
	 *         devices themselves will reflect back; modifications to the list will
	 *         not update anything in the project)
	 */
	public List<Device> getDevices() {
		return new ArrayList<>(devicesById.values());
	}

	/**
	 * @return the underlying <code>.knxproj</code> file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return a list of group addresses read from the KNX project (modifications to
	 *         the GAs themselves will reflect back; modifications to the list will
	 *         not update anything in the project)
	 */
	public List<GroupAddress> getGroupAddresses() {
		return new ArrayList<>(groupAddressById.values());
	}

	/**
	 * @return the internal project id used within the <code>.knxproj</code> file
	 */
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the project name
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Parses the underlying {@link #getFile()} and populates this object with data.
	 * <p>
	 * Must only be called once. Calling this method more than once may result in
	 * unexpected results. Callers should create a new instance instead.
	 * </p>
	 *
	 * @throws IOException        in case of issues reading {@link #getFile() from
	 *                            the file}
	 * @throws XMLStreamException in case of parsing errors (eg., invalid or missing
	 *                            data)
	 */
	public void open() throws IOException, XMLStreamException {
		LOG.info("Reading project: {}", file);

		// find and extract projects
		try (var zip = new ZipFile(file)) {
			var zipEntries = zip.getEntries();
			while (zipEntries.hasMoreElements()) {
				var zipEntry = zipEntries.nextElement();

				if (zipEntry.getName().endsWith("/project.xml") || zipEntry.getName().endsWith("/0.xml")) {
					LOG.debug("Analyzing zip entry: {}", zipEntry);
					var nameParts = zipEntry.getName().split("/");
					if (nameParts.length != 2) {
						LOG.warn("Found invalid zip entry: {}", zipEntry);
						continue;
					}

					if (!nameParts[0].startsWith("P-")) {
						LOG.warn("Found unsupported project id: {}", nameParts[0]);
						continue;
					}

					setProjectId(nameParts[0]);

					try (InputStream in = zip.getInputStream(zipEntry)) {
						if (nameParts[1].equals("project.xml")) {
							LOG.debug("Reading project info from: {}", zipEntry);
							readProjectInfo(in);
						} else if (nameParts[1].equals("0.xml")) {
							LOG.debug("Reading project data from: {}", zipEntry);
							readProjectData(in);
						}
					}
				}
			}
		}

		LOG.debug("Connecting devices and GAs");

		devicesById.values().parallelStream().forEach((device) -> {
			device.getCommunicationObjects().forEach((c) -> {
				if (c.getSendGroupAddressRefId() != null) {
					GroupAddress ga = groupAddressById.get(c.getSendGroupAddressRefId());
					c.setSendGroupAddress(ga);
					if (!ga.getWritingCommunicationObjects().contains(c)) {
						ga.getWritingCommunicationObjects().add(c);
					}
				}
				List<GroupAddress> listeningGas = c.getListenGroupAddressRefIds().stream()
						.map((id) -> groupAddressById.get(id)).collect(toList());
				c.getListenGroupAddresses().addAll(listeningGas);
				listeningGas.forEach((ga) -> {
					if (!ga.getListeningCommunicationObjects().contains(c)) {
						ga.getListeningCommunicationObjects().add(c);
					}
				});
			});
		});

		LOG.debug("Found {} devices and {} GAs", devicesById.size(), groupAddressById.size());
	}

	private void readArea(XMLStreamReader streamReader) throws XMLStreamException {
		String id = streamReader.getAttributeValue(null, "Id");
		String address = streamReader.getAttributeValue(null, "Address");
		String name = streamReader.getAttributeValue(null, "Name");
		String description = streamReader.getAttributeValue(null, "Description");

		var area = new Area(id, address, name, description);

		readElementChildren(streamReader, (childName) -> {
			if (childName.equals("Line")) {
				readLine(streamReader, area);
			}
		});
	}

	private void readDeviceInstance(XMLStreamReader streamReader, Line line) throws XMLStreamException {
		String id = streamReader.getAttributeValue(null, "Id");
		String address = streamReader.getAttributeValue(null, "Address");
		String name = streamReader.getAttributeValue(null, "Name");
		String description = streamReader.getAttributeValue(null, "Description");

		var device = new Device(line, id, address, name, description);
		line.getDevices().add(device);
		devicesById.put(id, device);

		LOG.debug("Found device: {}", device);

		readElementChildren(streamReader, (childName) -> {
			if (childName.equals("ComObjectInstanceRef")) {
				readDeviceInstanceComObject(streamReader, device);
			}
		});
	}

	private void readDeviceInstanceComObject(XMLStreamReader streamReader, Device device) throws XMLStreamException {
		String refId = streamReader.getAttributeValue(null, "RefId");
		String datapointType = convertToDpt(streamReader, streamReader.getAttributeValue(null, "DatapointType"));
		String description = streamReader.getAttributeValue(null, "Description");
		String readFlag = streamReader.getAttributeValue(null, "ReadFlag");

		var comObject = new CommunicationObject(device, refId, datapointType, description, "Enabled".equals(readFlag));
		device.getCommunicationObjects().add(comObject);

		readElementChildren(streamReader, (childName) -> {
			if (childName.equals("Send")) {
				comObject.setSendGroupAddressRefId(streamReader.getAttributeValue(null, "GroupAddressRefId"));
			} else if (childName.equals("Receive")) {
				comObject.getListenGroupAddressRefIds().add(streamReader.getAttributeValue(null, "GroupAddressRefId"));
			}
		});
	}

	private void readElementChildren(XMLStreamReader streamReader, ChildElementHandler nestedElementHandler)
			throws XMLStreamException {
		readElementChildrenLevel++;
		try {

			int level = 0;
			while (streamReader.hasNext()) {

				switch (streamReader.getEventType()) {
				case START_ELEMENT:
					readElementChildrenTrace("<{} ({})>", streamReader.getLocalName(), level);
					if (level > 0) {
						if (nestedElementHandler != null) {
							streamReader.getLocation();
							String nestedElementName = streamReader.getLocalName();
							nestedElementHandler.onChildElement(nestedElementName);
							readElementChildrenTrace("AFTER nestedElementHandler {}: {}", level,
									eventTypeName(streamReader.getEventType()));
							// a nested element handler is allowed to read the nested element completely,
							if ((streamReader.getEventType() == END_ELEMENT)
									&& streamReader.getLocalName().equals(nestedElementName)) {
								// in this case we don't increase the level
								streamReader.next(); // continue with next event
								break;
							}
						}
					}
					level++;
					streamReader.next(); // continue with next event
					break;

				case END_ELEMENT:
					level--;
					readElementChildrenTrace("</{} ({})>", streamReader.getLocalName(), level);
					if (level > 0) {
						// all good, continue reading
						streamReader.next();
						break;
					} else if (level < 0) {
						throw new XMLStreamException("Processed more END_ELEMENT than START_ELEMENT.",
								streamReader.getLocation());
					}
					readElementChildrenTrace("DONE {}: {}", level, streamReader.getLocalName());
					return; // done (and don't advance to next event)

				default:
					// ignore event
					streamReader.next();
					break;
				}
			}
		} finally {
			readElementChildrenLevel--;
		}
	}

	private void readElementChildrenTrace(String format, Object... arguments) {
		if (!LOG.isTraceEnabled()) {
			return;
		}

		String prefix = "";
		int width = (readElementChildrenLevel * 2) - 2;
		for (int i = 0; i < width; i++) {
			prefix += "  ";
		}

		LOG.trace(prefix + format, arguments);
	}

	private void readGroupAddress(XMLStreamReader streamReader, GroupAddressRange groupAddressRange)
			throws XMLStreamException {
		String id = streamReader.getAttributeValue(null, "Id");
		String name = streamReader.getAttributeValue(null, "Name");
		String description = streamReader.getAttributeValue(null, "Description");
		String datapointType = convertToDpt(streamReader, streamReader.getAttributeValue(null, "DatapointType"));

		int address;
		try {
			address = Integer.parseInt(streamReader.getAttributeValue(null, "Address"));
		} catch (NumberFormatException e) {
			throw new XMLStreamException("Invalid Address.", streamReader.getLocation(), e);
		}

		var groupAddress = new GroupAddress(groupAddressRange, id, address, name, description, datapointType);
		groupAddressById.put(id, groupAddress);

		LOG.debug("Found GA: {}", groupAddress);
	}

	private void readGroupAddresses(XMLStreamReader streamReader) throws XMLStreamException {
		readElementChildren(streamReader, (childName) -> {
			if (childName.equals("GroupRange")) {
				readGroupAddressRange(streamReader, null);
			}
		});
	}

	private void readGroupAddressRange(XMLStreamReader streamReader, GroupAddressRange parent)
			throws XMLStreamException {
		String id = streamReader.getAttributeValue(null, "Id");
		String name = streamReader.getAttributeValue(null, "Name");
		String description = streamReader.getAttributeValue(null, "Description");

		int start;
		try {
			start = Integer.parseInt(streamReader.getAttributeValue(null, "RangeStart"));
		} catch (NumberFormatException e) {
			throw new XMLStreamException("Invalid RangeStart.", streamReader.getLocation(), e);
		}

		int end;
		try {
			end = Integer.parseInt(streamReader.getAttributeValue(null, "RangeEnd"));
		} catch (NumberFormatException e) {
			throw new XMLStreamException("Invalid RangeEnd.", streamReader.getLocation(), e);
		}

		var groupAddressRange = new GroupAddressRange(parent, id, start, end, name, description);

		readElementChildren(streamReader, (elementName) -> {
			if (elementName.equals("GroupRange")) {
				readGroupAddressRange(streamReader, groupAddressRange);
			} else if (elementName.equals("GroupAddress")) {
				readGroupAddress(streamReader, groupAddressRange);
			}
		});
	}

	private void readLine(XMLStreamReader streamReader, Area area) throws XMLStreamException {
		String id = streamReader.getAttributeValue(null, "Id");
		String address = streamReader.getAttributeValue(null, "Address");
		String name = streamReader.getAttributeValue(null, "Name");
		String description = streamReader.getAttributeValue(null, "Description");

		var line = new Line(area, id, address, name, description);
		area.getLines().add(line);

		readElementChildren(streamReader, (childName) -> {
			if (childName.equals("DeviceInstance")) {
				readDeviceInstance(streamReader, line);
			}
		});
	}

	private void readLocations(XMLStreamReader streamReader) throws XMLStreamException {
		// TODO
	}

	void readProjectData(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = factory.createXMLStreamReader(in);

		while (streamReader.hasNext()) {
			streamReader.next();

			if (streamReader.getEventType() == START_ELEMENT) {
				String elementName = streamReader.getLocalName();
				if ("Project".equals(elementName)) {
					verifyProjectId(streamReader);
				} else if ("Topology".equals(elementName)) {
					readTopology(streamReader);
				} else if ("Locations".equals(elementName)) {
					readLocations(streamReader);
				} else if ("GroupAddresses".equals(elementName)) {
					readGroupAddresses(streamReader);
				}
			}
		}

		LOG.trace("Done reading project data.");
	}

	void readProjectInfo(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = factory.createXMLStreamReader(in);

		while (streamReader.hasNext()) {
			streamReader.next();

			if (streamReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				String elementName = streamReader.getLocalName();
				if ("Project".equals(elementName)) {
					verifyProjectId(streamReader);
				} else if ("ProjectInformation".equals(elementName)) {
					String name = streamReader.getAttributeValue(null, "Name");
					if (name == null) {
						throw new XMLStreamException("Missing Name on ProjectInformation element",
								streamReader.getLocation());
					}
					setProjectName(name);

					// done parsing
					LOG.trace("Done reading project info.");
					return;
				}
			}
		}

		LOG.warn("Abnormal finish. Incomplete or unsupported project info.");
	}

	private void readTopology(XMLStreamReader streamReader) throws XMLStreamException {
		readElementChildren(streamReader, (childName) -> {
			if (childName.equals("Area")) {
				readArea(streamReader);
			}
		});
	}

	private void setProjectId(String projectId) {
		if (this.projectId != null) {
			if (this.projectId.equals(projectId)) {
				LOG.trace("Project id already set!");
				return;
			}
			throw new IllegalStateException("Multiple projects not supported. Export only ONE project from ETS!");
		}

		this.projectId = projectId;
		LOG.debug("Using project id: {}", this.projectId);
	}

	private void setProjectName(String projectName) {
		this.projectName = projectName;
		LOG.debug("Using project name: {}", this.projectName);
	}

	@Override
	public String toString() {
		return format("KnxProjectFile [%s]", file.getAbsolutePath());
	}

	private void verifyProjectId(XMLStreamReader streamReader) throws XMLStreamException {
		String id = streamReader.getAttributeValue(null, "Id");
		if (id == null) {
			throw new XMLStreamException("Missing ID on Project element", streamReader.getLocation());
		}
		if (!id.equals(getProjectId())) {
			throw new XMLStreamException(
					format("Declared ID '%s' on Project element doesn't match expected ID '%s'", id, getProjectId()),
					streamReader.getLocation());
		}
	}
}

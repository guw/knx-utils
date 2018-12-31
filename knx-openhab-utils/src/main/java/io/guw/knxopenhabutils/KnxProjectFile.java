package io.guw.knxopenhabutils;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnxProjectFile {

	private static final Logger LOG = LoggerFactory.getLogger(KnxProjectFile.class);

	static void forwardToEndElement(XMLStreamReader streamReader, String elementName) throws XMLStreamException {
		while (streamReader.hasNext()) {
			streamReader.next();

			if (streamReader.getEventType() == XMLStreamConstants.END_ELEMENT) {
				if (elementName.equals(streamReader.getLocalName())) {
					return;
				}
			}
		}
		throw new XMLStreamException(format("Did not find closing tag for element '%s'", elementName));
	}

	private final File file;

	private String projectId;

	private String projectName;

	public KnxProjectFile(File knxProjFile) {
		file = knxProjFile;
	}

	public File getFile() {
		return file;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getProjectName() {
		return projectName;
	}

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
	}

	private void readProjectData(InputStream in) {
		// TODO Auto-generated method stub

	}

	private void readProjectInfo(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = factory.createXMLStreamReader(in);

		while (streamReader.hasNext()) {
			streamReader.next();

			if (streamReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				String elementName = streamReader.getLocalName();
				if ("Project".equals(elementName)) {
					String id = streamReader.getAttributeValue(null, "Id");
					if (id == null) {
						throw new XMLStreamException("Missing ID on Project element", streamReader.getLocation());
					}
					if (!id.equals(getProjectId())) {
						throw new XMLStreamException(
								format("Declared ID '%s' on Project element doesn't match expected ID '%s'", id,
										getProjectId()),
								streamReader.getLocation());
					}
				}
				if ("ProjectInformation".equals(elementName)) {
					String name = streamReader.getAttributeValue(null, "Name");
					if (name == null) {
						throw new XMLStreamException("Missing Name on ProjectInformation element",
								streamReader.getLocation());
					}
					setProjectName(name);

					// done parsing
					return;
				}
			}
		}
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
}

package io.guw.knxopenhabutils.semanticanalyzer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.guw.knxopenhabutils.knxprojectparser.CommunicationObject;
import io.guw.knxopenhabutils.knxprojectparser.DatapointType;
import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;

/**
 * The characteristics of a KNX project.
 * <p>
 * Characteristics describe and apply patterns found within a KNX project.
 * Although those characteristics are supposed to be general applicable to
 * multiple projects, this class is designed to be contain state specific to a
 * single KNX project. Such state may be a search index or another form of
 * knowledge dictionary. Therefore it should not be re-used across multiple
 * analysis sessions of different projects.
 * </p>
 */
public abstract class KnxProjectCharacteristics {

	private static final Logger LOG = LoggerFactory.getLogger(KnxProjectCharacteristics.class);
	private int warnings;

	/**
	 * Fills in missing information based on data available from the GA itself or
	 * implied by the characteristics.
	 * <p>
	 * This method will modify the specified {@link GroupAddress} directly. The
	 * default implementation will try to fill in missing
	 * {@link GroupAddress#getDatapointType()} if the GA is linked to a
	 * {@link CommunicationObject} with available DPT information.
	 * </p>
	 *
	 * @param ga the group address to complete
	 */
	public void fillInMissingInformation(GroupAddress ga) {
		for (CommunicationObject co : ga.getWritingCommunicationObjects()) {
			if (co.getDatapointType() != null) {
				if (ga.getDatapointType() == null) {
					LOG.debug("Update DPT to {} based on CO {} for GA {}", co.getDatapointType(), co, ga);
					ga.setDatapointType(co.getDatapointType());
				} else if (!ga.getDatapointType().equals(co.getDatapointType())) {
					LOG.warn("Found communication object with DPT {} which differs from expected {}\n  {}\n  GA {}",
							co.getDatapointType(), ga.getDatapointType(), co, ga);
					warnings++;
				}
			} else {
				LOG.warn("Found communication object without DPT\n  {}\n  GA {}", co, ga);
				warnings++;
			}
		}

		if ((ga.getName() == null) || ga.getName().isBlank()) {
			for (CommunicationObject co : ga.getWritingCommunicationObjects()) {
				if ((co.getDescription() != null) && !co.getDescription().isBlank()) {
					LOG.debug("Update name to '{}' based on CO {} for GA {}", co.getDescription(), co, ga);
					ga.setName(co.getDescription());
					break; // first one wins
				}
			}
		}

		if ((ga.getName() == null)
				|| ((ga.getName().isBlank()) && ((ga.getDescription() != null) && !ga.getDescription().isBlank()))) {
			LOG.debug("Update name to '{}' based on description for GA {}", ga.getDescription(), ga);
			ga.setName(ga.getDescription());
		}
	}

	int getWarnings() {
		return warnings;
	}

	/**
	 * Indicates if a GA is related to lighting (eg., switching, dimming, status
	 * etc.)
	 *
	 * @param ga a group address
	 * @return <code>true</code> if the specified GA is related to lighting
	 */
	public abstract boolean isLight(GroupAddress ga);

	/**
	 * Indicates if a GA is a primary GA responsible for switching a light on/off.
	 * <p>
	 * This is useful to allow grouping of GA related to another GA. For example,
	 * controlling a light requires at least two GAs - one for on/off and another
	 * one to indicate its status. A dimmable light may require two additional GAs,
	 * eg. one for brighter or darker, another one for setting brightness directly
	 * and a third for sending the brightness status back to the KNX bus. All in all
	 * five GAs. For analysis purposes, there is one primary GA and all others a
	 * considered secondary.
	 * </p>
	 * <p>
	 * The default implementation decides based on DPT. Subclasses may override an
	 * provide a more specific implementation.
	 * </p>
	 *
	 * @param ga a group address
	 * @return <code>true</code> if the specified GA is a primary
	 */
	public boolean isPrimarySwitch(GroupAddress ga) {
		DatapointType dpt = DatapointType.findByKnxProjectValue(ga.getDatapointType());
		if (dpt == null) {
			return false;
		}

		switch (dpt) {
		case Switch:
		case UpDown:
		case OpenClose:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Submits a GA to the characteristics for learning purposes.
	 * <p>
	 * The characteristics implementation may build an index of the GAs for more
	 * efficient processing.
	 * </p>
	 *
	 * @param groupAddresses list of addresses to learn
	 */
	public abstract void learn(List<GroupAddress> groupAddresses);
}

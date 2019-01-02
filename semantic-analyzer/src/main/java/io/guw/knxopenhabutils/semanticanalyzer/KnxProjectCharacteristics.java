package io.guw.knxopenhabutils.semanticanalyzer;

import java.util.List;

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

	/**
	 * Indicates if a GA is related to lighting (eg., switching, dimming, status
	 * etc.)
	 *
	 * @param ga a group address
	 * @return <code>true</code> if the specified GA is related to lighting
	 */
	public abstract boolean isLight(GroupAddress ga);

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

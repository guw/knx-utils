package io.guw.knxutils.semanticanalyzer;

import static io.guw.knxutils.knxprojectparser.DatapointType.ControlDimming;
import static io.guw.knxutils.knxprojectparser.DatapointType.Scaling;
import static io.guw.knxutils.knxprojectparser.GroupAddress.formatAsThreePartAddress;
import static io.guw.knxutils.knxprojectparser.GroupAddress.getAddressPart1;
import static io.guw.knxutils.knxprojectparser.GroupAddress.getAddressPart2;
import static io.guw.knxutils.knxprojectparser.GroupAddress.getAddressPart3;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.guw.knxutils.knxprojectparser.DatapointType;
import io.guw.knxutils.knxprojectparser.GroupAddress;
import io.guw.knxutils.knxprojectparser.GroupAddressRange;
import io.guw.knxutils.semanticanalyzer.luceneext.GermanAnalyzerWithDecompounder;

/**
 * Characteristics typically found in a KNX project using German language and
 * following best practises as published by KNX.org (aka. "KNX
 * Projektrichtlinien").
 */
public class GenericGermanyKnxProjectCharacteristics extends KnxProjectCharacteristics {

	public static class GroupAddressDocument {
		public Set<String> nameTerms;
	}

	private static final Logger LOG = LoggerFactory.getLogger(GenericGermanyKnxProjectCharacteristics.class);

	private static final Analyzer germanAnalyzer = new GermanAnalyzerWithDecompounder();
	private static final Set<String> lightIdentifyingTerms = Set.of("licht", "leucht", "beleuchtung", "lamp", "spot",
			"strahl");
	private static final List<String> lightIdentifyingPrefixes = List.of("L_", "LD_", "LDA_");

	private final Map<GroupAddress, GroupAddressDocument> groupAddressIndex = new HashMap<>();
	private final Map<GroupAddressRange, GroupAddressDocument> groupAddressRangeIndex = new HashMap<>();
	private final Map<String, GroupAddress> groupAddressByThreePartAddress = new HashMap<>();

	float calculatePrefixMatchRatio(String candidateName, String primaryName) {
		// simple heuristic based on prefix matching
		int minLength = Math.min(candidateName.length(), primaryName.length());
		int commonPrefixLength = 0;
		for (; commonPrefixLength < minLength; commonPrefixLength++) {
			if (candidateName.charAt(commonPrefixLength) != primaryName.charAt(commonPrefixLength)) {
				break;
			}
		}
		return (float) commonPrefixLength / (float) minLength;
	}

	private boolean containsStatusTerm(Set<String> terms) {
		return terms.contains("status") || terms.contains("ruckmeldung");
	}

	private boolean descriptionContainsTag(GroupAddress ga, String tag) {
		return (ga.getDescription() != null) && ga.getDescription().contains(tag);
	}

	/**
	 * Returns potential candidates of a block.
	 * <p>
	 * Note, the returned list does not include the
	 * <code>startingGroupAddress</code>. Thus, it is guaranteed to has a maximum
	 * size of <code>blockLength - 1</code>. For example is start address is
	 * <code>1/0/0</code>,
	 * the result will contain <code>1/0/1</code>, <code>1/0/2</code>,
	 * <code>1/0/3</code> and <code>1/0/4</code>, assuming their names
	 * are similar enough.
	 * </p>
	 *
	 * @param startingGroupAddress a start address
	 * @param blockLength          the block length
	 * @return the candidates of a potential block with maximum block length as
	 *         specified, excluding the given start address
	 */
	private List<GroupAddress> findGroupAddressBlockCandidates(GroupAddress startingGroupAddress, int blockLength) {
		List<GroupAddress> block = new ArrayList<>(blockLength);
		for (int i = 1; i < blockLength; i++) {
			GroupAddress candidate = groupAddressByThreePartAddress
					.get(GroupAddress.formatAsThreePartAddress(startingGroupAddress.getAddressInt() + i));
			if (candidate != null) {
				if (!isMatchOnName(candidate, startingGroupAddress)) {
					LOG.debug(
							"Project doesn't seem to use expected block structure. Insignificant name matching for GA {} (with name '{}') and candidate GA {} with name '{}'.",
							startingGroupAddress, startingGroupAddress.getName(), candidate, candidate.getName());
					return block;
				}
				block.add(candidate);
			}
		}
		return block;
	}

	@Override
	public GroupAddress findMatchingBrightnessGroupAddress(GroupAddress primarySwitchGroupAddress) {
		// pattern 1: assume GAs a created as blocks of 5 GAs (0=OnOff, 1=Dim, 2=Value, 3=StatusOnOff, 4=StatusValue)
		// TODO: this should be configurable
		GroupAddress candidate = groupAddressByThreePartAddress
				.get(GroupAddress.formatAsThreePartAddress(primarySwitchGroupAddress.getAddressInt() + 2));
		if (candidate != null) {
			LOG.debug("Evaluating potential candidate for GA {}: {}", primarySwitchGroupAddress, candidate);
			if (isMatchOnNameAndDpt(candidate, primarySwitchGroupAddress, Scaling)) {
				return candidate;
			}
		}

		// give up
		return null;
	}

	@Override
	public GroupAddress findMatchingBrightnessStatusGroupAddress(GroupAddress primarySwitchGroupAddress) {
		// pattern 1: assume GAs a created as blocks of 5 GAs (0=OnOff, 1=Dim, 2=Value, 3=StatusOnOff, 4=StatusValue)
		// TODO: this should be configurable
		GroupAddress candidate = groupAddressByThreePartAddress
				.get(GroupAddress.formatAsThreePartAddress(primarySwitchGroupAddress.getAddressInt() + 4));
		if (candidate != null) {
			LOG.debug("Evaluating potential candidate for GA {}: {}", primarySwitchGroupAddress, candidate);
			if (isMatchOnNameAndDpt(candidate, primarySwitchGroupAddress, Scaling)) {
				return candidate;
			}
		}

		// give up
		return null;
	}

	@Override
	public GroupAddress findMatchingDimGroupAddress(GroupAddress primarySwitchGroupAddress) {
		// pattern 1: assume GAs a created as blocks of 5 GAs (0=OnOff, 1=Dim, 2=Value, 3=StatusOnOff, 4=StatusValue)
		// TODO: this should be configurable
		GroupAddress candidate = groupAddressByThreePartAddress
				.get(GroupAddress.formatAsThreePartAddress(primarySwitchGroupAddress.getAddressInt() + 1));
		if (candidate != null) {
			LOG.debug("Evaluating potential candidate for GA {}: {}", primarySwitchGroupAddress, candidate);
			if (isMatchOnNameAndDpt(candidate, primarySwitchGroupAddress, ControlDimming)) {
				return candidate;
			}
		}

		// give up
		return null;
	}

	@Override
	public GroupAddress findMatchingStatusGroupAddress(GroupAddress primarySwitchGroupAddress) {
		// preselect based on common patterns
		List<GroupAddress> candidates = findGroupAddressBlockCandidates(primarySwitchGroupAddress, 5).stream()
				.filter(ga -> DatapointType.findByKnxProjectValue(ga.getDatapointType()) == DatapointType.State)
				.collect(toList());
		if (!candidates.isEmpty()) {
			if (candidates.size() == 1) {
				GroupAddress candidate = candidates.get(0);
				LOG.debug("Found matching status for GA {}: {}", primarySwitchGroupAddress, candidate);
				return candidate;
			}
			LOG.warn("Project is ambiguous. Found multiple matches with DPT {} for GA {}: {}", DatapointType.State,
					primarySwitchGroupAddress, candidates.stream().map(GroupAddress::toString).collect(joining(", ")));
		} else {
			LOG.debug("No candidate indentified with DPT {} based on block pattern for GA {}", DatapointType.State,
					primarySwitchGroupAddress);
		}

		// pattern 2: status GA is in a different range
		List<GroupAddressRange> statusRanges = groupAddressRangeIndex.entrySet().stream()
				.filter((e) -> containsStatusTerm(e.getValue().nameTerms)).map(Entry::getKey).collect(toList());
		for (GroupAddressRange statusRange : statusRanges) {
			int part1, part2, part3;
			if (statusRange.getParent() == null) {
				part1 = getAddressPart1(statusRange.getStartInt());
				part2 = getAddressPart2(primarySwitchGroupAddress.getAddressInt());
				part3 = getAddressPart3(primarySwitchGroupAddress.getAddressInt());
			} else {
				part1 = getAddressPart1(primarySwitchGroupAddress.getAddressInt());
				part2 = getAddressPart2(statusRange.getStartInt());
				part3 = getAddressPart3(primarySwitchGroupAddress.getAddressInt());
			}
			String potentialStatusGa = formatAsThreePartAddress(part1, part2, part3);
			GroupAddress candidate = groupAddressByThreePartAddress.get(potentialStatusGa);
			if (candidate != null) {
				LOG.debug("Evaluating potential candidate for GA {}: {}", primarySwitchGroupAddress, candidate);
				if (isMatchOnNameAndDpt(candidate, primarySwitchGroupAddress, DatapointType.State)) {
					LOG.debug("Found matching status for GA {}: {}", primarySwitchGroupAddress, candidate);
					return candidate;
				}
			}
		}

		// give up
		return null;
	}

	@Override
	public String findName(GroupAddress primaryGroupAddress, GroupAddress... additionalGroupAddresses) {
		String name = primaryGroupAddress.getName();
		START_AGAIN: if (additionalGroupAddresses != null) {
			for (GroupAddress ga : additionalGroupAddresses) {
				if (!ga.getName().startsWith(name)) {
					int lastSpace = name.lastIndexOf(' ');
					if (lastSpace > 0) {
						name = name.substring(0, lastSpace);
						break START_AGAIN;
					} else if (name.length() > 0) {
						name = name.substring(0, name.length() - 1);
						break START_AGAIN;
					} else {
						// give up, no common name
						return primaryGroupAddress.getName();
					}
				}
			}
		}
		return name;
	}

	Set<String> getTerms(String text) throws IOException {
		Set<String> terms = new LinkedHashSet<>(); // make sure we maintain order
		try (TokenStream ts = germanAnalyzer.tokenStream("", text)) {
			CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
			ts.reset();
			while (ts.incrementToken()) {
				terms.add(charTermAttribute.toString());
			}
			ts.end();
		}
		return terms;
	}

	private void index(GroupAddress ga) {
		try {
			GroupAddressDocument doc = new GroupAddressDocument();
			doc.nameTerms = getTerms(ga.getName());
			groupAddressIndex.put(ga, doc);

			GroupAddressRange range = ga.getGroupAddressRange();
			while ((range != null) && !groupAddressRangeIndex.containsKey(range)) {
				index(range);
				range = range.getParent();
			}
		} catch (IOException e) {
			LOG.warn("Caught exception indexing GA {}", ga, e);
		}
	}

	private void index(GroupAddressRange range) {
		try {
			GroupAddressDocument rangeDoc = new GroupAddressDocument();
			rangeDoc.nameTerms = getTerms(range.getName());
			groupAddressRangeIndex.put(range, rangeDoc);
		} catch (IOException e) {
			LOG.warn("Caught exception indexing group address range {}", range, e);
		}
	}

	@Override
	public boolean isLight(GroupAddress ga) {
		if ((null == ga.getName()) || ga.getName().isBlank()) {
			LOG.warn("GA with blank/empty name should be fixed: {}", ga);
			return false;
		}

		GroupAddressDocument doc = groupAddressIndex.get(ga);
		if (doc == null) {
			LOG.warn("No index available for GA: {}", ga);
			return false;
		}

		return nameContainsTerms(doc, lightIdentifyingTerms) || nameStartsWith(ga, lightIdentifyingPrefixes)
				|| descriptionContainsTag(ga, "[Licht]");
	}

	boolean isMatchOnName(GroupAddress candidate, GroupAddress ga) {
		// prefix match
		// TODO: this should be configurable
		float prefixMatchRatio = calculatePrefixMatchRatio(candidate.getName(), ga.getName());
		if (prefixMatchRatio <= 0.6F) {
			LOG.debug(
					"Prefix mismatch for candidate {} with name '{}' comparing to primary {} with name '{}' (match {})",
					candidate, candidate.getName(), ga, ga.getName(), prefixMatchRatio);
			return false;
		}

		return true;
	}

	boolean isMatchOnNameAndDpt(GroupAddress candidate, GroupAddress primarySwitchGroupAddress, DatapointType dpt,
			DatapointType... dpts) {
		// prefix match
		if (!isMatchOnName(candidate, primarySwitchGroupAddress)) {
			return false;
		}

		// DPT match
		if ((candidate.getDatapointType() == null) || candidate.getDatapointType().isBlank()) {
			// TODO: this should be configurable
			LOG.warn("Accepting candidate with missing DPT {}", candidate);
			return true;
		}
		DatapointType candidateDpt = DatapointType.findByKnxProjectValue(candidate.getDatapointType());
		return (dpt == candidateDpt) || ((null != dpts) && (Arrays.stream(dpts).anyMatch((d) -> candidateDpt == d)));
	}

	@Override
	public boolean isPrimarySwitch(GroupAddress ga) {
		// pre-select based on DPT
		boolean isPotentialPrimary = super.isPrimarySwitch(ga);
		if (!isPotentialPrimary) {
			LOG.debug("Not a primary switch GA due to DPT mismatch: {}", ga);
			return false;
		}

		// filter out addresses with similar name but not primary purpose (eg., status)
		GroupAddressDocument doc = groupAddressIndex.get(ga);
		if (doc == null) {
			LOG.warn("No index available for GA: {}", ga);
			return false;
		}

		return !containsStatusTerm(doc.nameTerms);
	}

	@Override
	public void learn(List<GroupAddress> groupAddresses) {
		for (GroupAddress ga : groupAddresses) {
			index(ga);
			groupAddressByThreePartAddress.put(ga.getAddress(), ga);
		}
	}

	private boolean nameContainsTerms(GroupAddressDocument doc, Set<String> terms) {
		return doc.nameTerms.parallelStream().filter((s) -> terms.contains(s)).findAny().isPresent();
	}

	private boolean nameStartsWith(GroupAddress ga, List<String> prefix) {
		return prefix.parallelStream().filter((p) -> ga.getName().startsWith(p)).findAny().isPresent();
	}

}

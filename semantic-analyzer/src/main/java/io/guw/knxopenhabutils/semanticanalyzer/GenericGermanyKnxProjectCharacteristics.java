package io.guw.knxopenhabutils.semanticanalyzer;

import static io.guw.knxopenhabutils.knxprojectparser.DatapointType.ControlDimming;
import static io.guw.knxopenhabutils.knxprojectparser.DatapointType.Scaling;
import static io.guw.knxopenhabutils.knxprojectparser.GroupAddress.formatAsThreePartAddress;
import static io.guw.knxopenhabutils.knxprojectparser.GroupAddress.getAddressPart1;
import static io.guw.knxopenhabutils.knxprojectparser.GroupAddress.getAddressPart2;
import static io.guw.knxopenhabutils.knxprojectparser.GroupAddress.getAddressPart3;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.guw.knxopenhabutils.knxprojectparser.DatapointType;
import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;
import io.guw.knxopenhabutils.knxprojectparser.GroupAddressRange;
import io.guw.knxopenhabutils.semanticanalyzer.luceneext.GermanAnalyzerWithDecompounder;

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

		// pattern 1: assume GAs a created as blocks of 5 GAs (0=OnOff, 1=Dim, 2=Value, 3=StatusOnOff, 4=StatusValue)
		// TODO: this should be configurable
		GroupAddress candidate = groupAddressByThreePartAddress
				.get(GroupAddress.formatAsThreePartAddress(primarySwitchGroupAddress.getAddressInt() + 3));
		if (candidate != null) {
			LOG.debug("Evaluating potential candidate for GA {}: {}", primarySwitchGroupAddress, candidate);
			if (isMatchingStatus(candidate, primarySwitchGroupAddress)) {
				return candidate;
			}
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
			candidate = groupAddressByThreePartAddress.get(potentialStatusGa);
			if (candidate != null) {
				LOG.debug("Evaluating potential candidate for GA {}: {}", primarySwitchGroupAddress, candidate);
				if (isMatchingStatus(candidate, primarySwitchGroupAddress)) {
					return candidate;
				}
			}
		}

		// give up
		return null;
	}

	Set<String> getTerms(String text) throws IOException {
		Set<String> terms = new HashSet<>();
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

	boolean isMatchingStatus(GroupAddress candidate, GroupAddress primarySwitchGroupAddress) {
		// prefix match
		// TODO: this should be configurable
		float prefixMatchRatio = calculatePrefixMatchRatio(candidate.getName(), primarySwitchGroupAddress.getName());
		if (prefixMatchRatio <= 0.6F) {
			LOG.debug("Prefix mismatch for candidate {} for primary {} (match {})", candidate,
					primarySwitchGroupAddress, prefixMatchRatio);
			return false;
		}

		// accept all 1-bit DPTs
		if ((candidate.getDatapointType() == null) || candidate.getDatapointType().isBlank()) {
			// TODO: this should be configurable
			LOG.warn("Accepting candidate with missing DPT {}", candidate);
			return true;
		}
		return candidate.getDatapointType().startsWith("1.");
	}

	boolean isMatchOnNameAndDpt(GroupAddress candidate, GroupAddress primarySwitchGroupAddress, DatapointType dpt,
			DatapointType... dpts) {
		// prefix match
		// TODO: this should be configurable
		float prefixMatchRatio = calculatePrefixMatchRatio(candidate.getName(), primarySwitchGroupAddress.getName());
		if (prefixMatchRatio <= 0.6F) {
			LOG.debug("Prefix mismatch for candidate {} for primary {} (match {})", candidate,
					primarySwitchGroupAddress, prefixMatchRatio);
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
		// pre-select based on super
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

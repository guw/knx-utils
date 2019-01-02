package io.guw.knxopenhabutils.semanticanalyzer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.guw.knxopenhabutils.knxprojectparser.GroupAddress;
import io.guw.knxopenhabutils.semanticanalyzer.luceneext.GermanAnalyzerWithDecompounder;

public class GenericGermanyKnxProjectCharacteristics extends KnxProjectCharacteristics {

	public static class GroupAddressDocument {
		public Set<String> nameTerms;
	}

	private static final Logger LOG = LoggerFactory.getLogger(GenericGermanyKnxProjectCharacteristics.class);

	private static final Analyzer germanAnalyzer = new GermanAnalyzerWithDecompounder();

	private final Map<GroupAddress, GroupAddressDocument> groupAddressIndex = new HashMap<>();

	private Set<String> getTerms(GroupAddress ga, String text) throws IOException {
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
			doc.nameTerms = getTerms(ga, ga.getName());
			groupAddressIndex.put(ga, doc);
		} catch (IOException e) {
			LOG.warn("Caught exception indexing GA {}", ga, e);
		}
	}

	@Override
	public boolean isLight(GroupAddress ga) {
		if ((null == ga.getName()) || ga.getName().isBlank()) {
			return false;
		}

		GroupAddressDocument doc = groupAddressIndex.get(ga);
		if (doc == null) {
			LOG.warn("No index available for GA: {}", ga);
			return false;
		}

		return doc.nameTerms.contains("licht") || doc.nameTerms.contains("leucht");
	}

	@Override
	public void learn(List<GroupAddress> groupAddresses) {
		for (GroupAddress ga : groupAddresses) {
			index(ga);
		}
	}

}

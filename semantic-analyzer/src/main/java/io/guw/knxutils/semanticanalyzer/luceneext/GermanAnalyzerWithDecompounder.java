/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.guw.knxutils.semanticanalyzer.luceneext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.compound.CompoundWordTokenFilterBase;
import org.apache.lucene.analysis.compound.HyphenationCompoundWordTokenFilter;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.apache.lucene.analysis.de.GermanLightStemFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * {@link Analyzer} for German language with decompounder functionality.
 * <p>
 * Forked from {@link org.apache.lucene.analysis.de.GermanAnalyzer}.
 * </p>
 * <p>
 * Modified to include {@link HyphenationCompoundWordTokenFilter} and custom
 * stop
 * words. In order to use this, the Git repo at
 * <code>https://github.com/uschindler/german-decompounder</code> must be cloned
 * to local disc. This is required because of licensing issues.
 * </p>
 *
 * @see org.apache.lucene.analysis.de.GermanAnalyzer
 */
public final class GermanAnalyzerWithDecompounder extends StopwordAnalyzerBase {

	private static class DefaultSetHolder {
		private static final String DOWNLOAD_ADVICE = NEWLINE + NEWLINE + NEWLINE
				+ "  >>>  Please run 'mvn -Pdownload-german-data generate-sources' in 'semantic-analyzer' Maven module to enable it."
				+ NEWLINE + NEWLINE + NEWLINE;

		private static final CharArraySet DEFAULT_STOPWORD_SET;
		private static CharArraySet compoundDictionary;
		private static HyphenationTree hyphenator;
		static {
			try {
				DEFAULT_STOPWORD_SET = WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(
						GermanAnalyzerWithDecompounder.class, DEFAULT_STOPWORD_FILE, StandardCharsets.UTF_8));
			} catch (IOException ex) {
				// default set should always be present as it is part of the
				// distribution (JAR)
				throw new RuntimeException("Unable to load default stopword set", ex);
			}

			try (InputStream stream = GermanAnalyzerWithDecompounder.class
					.getResourceAsStream(DEFAULT_DICTIONARY_FILE)) {
				if (stream != null) {
					compoundDictionary = WordlistLoader.getWordSet(IOUtils.getDecodingReader(
							GermanAnalyzerWithDecompounder.class, DEFAULT_DICTIONARY_FILE, StandardCharsets.UTF_8));
				} else {
					LOG.warn(
							"Unable to load dictionary for HyphenationCompoundWordTokenFilter. Feature will be disabled."
									+ DOWNLOAD_ADVICE);
				}
			} catch (IOException ex) {
				throw new RuntimeException("Unable to load default dictionary set", ex);
			}

			try (InputStream stream = GermanAnalyzerWithDecompounder.class
					.getResourceAsStream(DEFAULT_HYPHENATOR_FILE)) {
				if (stream != null) {
					hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(new InputSource(stream));
				} else {
					LOG.warn(
							"Unable to load hyphenator for HyphenationCompoundWordTokenFilter. Feature will be disabled."
									+ DOWNLOAD_ADVICE);
				}
			} catch (IOException ex) {
				throw new RuntimeException("Unable to load default hyphenator tree", ex);
			}

		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(GermanAnalyzerWithDecompounder.class);

	private static final String NEWLINE = System.getProperty("line.separator");

	/** File containing default German stopwords. */
	public final static String DEFAULT_STOPWORD_FILE = "german_stop-modified.txt";

	/** File containing default German hyphenator. */
	public final static String DEFAULT_HYPHENATOR_FILE = "de_DR.xml";

	/** File containing default German dictionary. */
	public final static String DEFAULT_DICTIONARY_FILE = "dictionary-de.txt";

	/**
	 * Returns a set of default German-stopwords
	 *
	 * @return a set of default German-stopwords
	 */
	public static CharArraySet getDefaultStopSet() {
		return DefaultSetHolder.DEFAULT_STOPWORD_SET;
	}

	/**
	 * Contains the stopwords used with the {@link StopFilter}.
	 */

	/**
	 * Contains words that should be indexed but not stemmed.
	 */
	private final CharArraySet exclusionSet;

	private final HyphenationTree hyphenator;

	private final CharArraySet dictionary;

	/**
	 * Builds an analyzer with the default stop words:
	 * {@link #getDefaultStopSet()}.
	 */
	public GermanAnalyzerWithDecompounder() {
		this(DefaultSetHolder.DEFAULT_STOPWORD_SET);
	}

	/**
	 * Builds an analyzer with the given stop words
	 *
	 * @param stopwords
	 *                  a stopword set
	 */
	public GermanAnalyzerWithDecompounder(CharArraySet stopwords) {
		this(stopwords, CharArraySet.EMPTY_SET);
	}

	/**
	 * Builds an analyzer with the given stop words
	 *
	 * @param stopwords
	 *                         a stopword set
	 * @param stemExclusionSet
	 *                         a stemming exclusion set
	 */
	public GermanAnalyzerWithDecompounder(CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		exclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
		hyphenator = DefaultSetHolder.hyphenator;
		dictionary = DefaultSetHolder.compoundDictionary;
	}

	/**
	 * Creates
	 * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 * used to tokenize all the text in the provided {@link Reader}.
	 *
	 * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 *         built from a {@link StandardTokenizer} filtered with
	 *         {@link LowerCaseFilter}, {@link StopFilter}
	 *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is
	 *         provided, {@link GermanNormalizationFilter} and
	 *         {@link GermanLightStemFilter}
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardTokenizer();
		TokenStream result = new LowerCaseFilter(source);
		result = new StopFilter(result, stopwords);
		result = new SetKeywordMarkerFilter(result, exclusionSet);
		if ((DefaultSetHolder.hyphenator != null) && (DefaultSetHolder.compoundDictionary != null)) {
			result = new HyphenationCompoundWordTokenFilter(result, hyphenator, dictionary,
					CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE, 4,
					CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, true);
		}
		result = new GermanNormalizationFilter(result);
		result = new GermanLightStemFilter(result);
		return new TokenStreamComponents(source, result);
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		TokenStream result = new LowerCaseFilter(in);
		result = new GermanNormalizationFilter(result);
		return result;
	}
}

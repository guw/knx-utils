package io.guw.knxutils.homeassistant;

import java.io.IOException;

/**
 * A {@link StringBuilder} for creating YAML output
 */
public class YamlBuilder implements Appendable {

	private final StringBuilder stringBuilder = new StringBuilder();

	private final int indentSize;
	private final int indentLevel = 0;
	private boolean mustIndent;

	public YamlBuilder(int indentSize) {
		this.indentSize = indentSize;
		mustIndent = true;
	}

	@Override
	public YamlBuilder append(char c) throws IOException {
		append((int) c);
		return this;
	}

	@Override
	public YamlBuilder append(CharSequence csq) throws IOException {
		csq.chars().forEachOrdered(this::append);
		return this;
	}

	@Override
	public YamlBuilder append(CharSequence csq, int start, int end) throws IOException {
		csq.subSequence(start, end).chars().forEachOrdered(this::append);
		return this;
	}

	private void append(int codePoint) {
		if (mustIndent) {
			stringBuilder.append(" ".repeat(indentLevel * indentSize));
			mustIndent = false;
		}

		switch (codePoint) {
		case '\n':
			stringBuilder.append(System.lineSeparator());
			mustIndent = true;
			break;

		case '\r':
			// filter out
			break;

		default:
			stringBuilder.appendCodePoint(codePoint);
			break;
		}
	}
}

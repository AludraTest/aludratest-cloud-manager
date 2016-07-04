package org.aludratest.cloud.web.report;

public class JavaScriptString extends AbstractJavaScriptElement {

	private String value;

	public JavaScriptString(String value) {
		this.value = value;
	}

	@Override
	public void appendTo(StringBuilder sb) {
		sb.append("\"");

		// TODO & -> &amp;?

		for (char c : value.toCharArray()) {
			switch (c) {
				case '\r':
					sb.append("\\r");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\"':
					sb.append("\\\"");
					break;
				default:
					sb.append(c);
					break;
			}
		}

		sb.append("\"");
	}

}

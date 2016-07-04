package org.aludratest.cloud.web.report;

import java.util.LinkedHashMap;
import java.util.Map;

public class JavaScriptObject extends AbstractJavaScriptElement {

	private Map<String, JavaScriptElement> content = new LinkedHashMap<String, JavaScriptElement>();

	public void set(String key, JavaScriptElement value) {
		content.put(key, value);
	}

	@Override
	public void appendTo(StringBuilder sb) {
		sb.append("{");
		boolean first = true;

		for (Map.Entry<String, JavaScriptElement> entry : content.entrySet()) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(entry.getKey()).append(": ");
			entry.getValue().appendTo(sb);
		}

		sb.append("}");
	}

}

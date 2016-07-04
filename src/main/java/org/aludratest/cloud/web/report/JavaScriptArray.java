package org.aludratest.cloud.web.report;

import java.util.ArrayList;
import java.util.List;

public class JavaScriptArray extends AbstractJavaScriptElement {

	private List<JavaScriptElement> values = new ArrayList<JavaScriptElement>();

	public JavaScriptArray add(JavaScriptElement object) {
		values.add(object);
		return this;
	}

	@Override
	public void appendTo(StringBuilder sb) {
		sb.append("[");
		boolean first = true;
		for (JavaScriptElement obj : values) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(obj.toString());
		}
		sb.append("]");
	}

}

package org.aludratest.cloud.web.report;

public abstract class AbstractJavaScriptElement implements JavaScriptElement {

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}

}

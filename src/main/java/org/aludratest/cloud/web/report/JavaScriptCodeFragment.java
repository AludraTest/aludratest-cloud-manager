package org.aludratest.cloud.web.report;

public class JavaScriptCodeFragment extends AbstractJavaScriptElement {

	private String jsCode;

	public JavaScriptCodeFragment(String jsCode) {
		this.jsCode = jsCode;
	}

	@Override
	public void appendTo(StringBuilder sb) {
		sb.append(jsCode);
	}

}

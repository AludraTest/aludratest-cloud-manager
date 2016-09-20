/*
 * Copyright (C) 2015 Hamburg Sud and the contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

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

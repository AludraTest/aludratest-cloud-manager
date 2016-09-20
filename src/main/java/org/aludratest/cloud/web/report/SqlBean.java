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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.RowSet;

import org.aludratest.cloud.impl.app.CloudManagerApplicationHolder;
import org.aludratest.cloud.util.JSFUtil;
import org.apache.commons.io.IOUtils;

@ManagedBean(name = "sqlBean")
@ViewScoped
public class SqlBean implements Serializable {

	private static final long serialVersionUID = 4399029120167312756L;

	private String sql;

	private List<String> resultColumns;

	private List<Map<String, String>> resultRows;

	public SqlBean() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt = new Date(new Date().getTime() - (1000l * 60 * 60 * 24 * 3));

		sql = "SELECT * FROM acm_request WHERE start_wait_time_utc > '" + sdf.format(dt) + "'";
	}

	public synchronized void execute() {
		resultColumns = null;
		resultRows = null;

		if (sql == null) {
			return;
		}

		sql = sql.trim();
		while (sql.endsWith(";")) {
			sql = sql.substring(0, sql.length() - 1);
		}

		// easy basic check
		if (!sql.toLowerCase(Locale.US).startsWith("select ")) {
			FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createErrorMessage("Only SELECT statements are allowed"));
			return;
		}

		try {
			RowSet rs = CloudManagerApplicationHolder.getInstance().getDatabase().populateQuery(sql);

			resultColumns = new ArrayList<String>();
			ResultSetMetaData meta = rs.getMetaData();
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				resultColumns.add(meta.getColumnName(i));
			}

			rs.beforeFirst();
			resultRows = new ArrayList<Map<String, String>>();

			while (rs.next()) {
				Map<String, String> row = new HashMap<String, String>();

				// TODO nicer formatting etc.
				for (String col : resultColumns) {
					row.put(col, rs.getString(col));
				}

				resultRows.add(row);
			}
		}
		catch (SQLException se) {
			FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createErrorMessage("SQLException: " + se.getMessage()));
		}
	}

	public void download() {
		FacesContext context = FacesContext.getCurrentInstance();

		// export as CSV - directly send to response to save memory, but use gzip encoding if supported
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		String acceptHeader = request.getHeader("Accept-Encoding");
		boolean gzipSupported = acceptHeader != null && acceptHeader.contains("gzip");

		HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
		response.setContentType("text/csv");
		response.setCharacterEncoding("UTF-8");

		DateFormat dfFileMarker = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
		response.setHeader("Content-Disposition", "attachment; filename=acm-data-" + dfFileMarker.format(new Date()) + ".csv");

		if (gzipSupported) {
			response.setHeader("Content-Encoding", "gzip");
		}

		OutputStream out = null;
		try {
			out = response.getOutputStream();
			if (gzipSupported) {
				out = new GZIPOutputStream(out, 2048);
			}
			
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			
			// write header
			for (String colName : resultColumns) {
				writer.append(colName).append(";");
			}
			writer.write("\n");
			
			// write data
			for (Map<String, String> row : resultRows) {
				for (String key : resultColumns) {
					String value = row.get(key);
					if (value != null) {
						writer.write(value);
					}
					writer.write(";");
				}
				writer.write("\n");
			}
			
			writer.flush();
			writer.close();
		}
		catch (IOException e) {
			// ignore for now
		}
		finally {
			IOUtils.closeQuietly(out);
		}

		context.responseComplete();
	}


	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}
	
	public List<String> getResultColumns() {
		return resultColumns;
	}
	
	public List<Map<String, String>> getResultRows() {
		return resultRows;
	}
	
}

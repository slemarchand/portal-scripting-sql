/**
 * Copyright (c) 2012-2014 Sébastien Le Marchand, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.slemarchand.sqlqueryscripting.scripting.sqlquery;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.io.unsync.UnsyncPrintWriter;
import com.liferay.portal.kernel.scripting.ExecutionException;
import com.liferay.portal.kernel.scripting.ScriptingException;
import com.liferay.portal.kernel.scripting.ScriptingExecutor;
import com.liferay.portal.kernel.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;

/**
 * @author Sébastien Le Marchand
 */
public class SQLQueryExecutor implements ScriptingExecutor {

	public static final String LANGUAGE = "sql-query";

	public void clearCache() {

		// Nothing to do
	}

	public String getLanguage() {

		return LANGUAGE;
	}

	public Map<String, Object> eval(
			Set<String> allowedClasses, Map<String, Object> inputObjects,
			Set<String> outputNames, File script, ClassLoader... classLoaders)
		throws ScriptingException {
		try {
			return eval(allowedClasses, inputObjects, outputNames, FileUtil.read(script));
		}
		catch (IOException e) {
			throw new ScriptingException(e);
		}
	}
	
	public Map<String, Object> eval(
		Set<String> allowedClasses, Map<String, Object> inputObjects,
		Set<String> outputNames, String script, ClassLoader... classLoaders)
		throws ScriptingException {

		if (allowedClasses != null) {
			throw new ExecutionException(
				"Constrained execution not supported for database queries");
		}

		UnsyncPrintWriter out = (UnsyncPrintWriter) inputObjects.get("out");
		
		Map<String, String> hints = _getHints(script);

		boolean emptyScript = script.replace("--help", "").trim().length() == 0;
		
		boolean help = _getHint("help", hints, null) != null;
		
		if(help || emptyScript) {
			_displayHelp(out);
		} else {
			out.append("To learn more about execution parameters, add \"--help\" line in your script or execute empty script\n\n");
		}
		
		if(!emptyScript) {
		
			int maxRows = _getIntegerHint("maxRows", hints, 50);
	
			List<String> columnLabels = new LinkedList<String>();
			List<List<Object>> rows = null;
			
			try {
				rows = _execQuery(script, maxRows, columnLabels);
			}
			catch (SQLException e) {
				throw new ScriptingException(e);
			}
			
			String format = _getHint("format", hints, "html");
	
			if ("html".equalsIgnoreCase(format)) {
				_formatHTML(columnLabels, rows, out);
			}
			else if ("csv".equalsIgnoreCase(format)) {
				_formatCSV(columnLabels, rows, out);
			}
			else {
				throw new ScriptingException("Unknown value '" + format + "' for hint 'format'");
			}
		
		}

		Map<String, Object> outputObjects; 
		
		if (outputNames != null) {
			// Output objects not supported
			outputObjects = new HashMap<String, Object>();
		} else {
			outputObjects = null;
		}
		
		return outputObjects;
	}

	private Map _getHints(String sqlQuery)
		throws ScriptingException {

		Properties props = new Properties();

		try {

			List<String> lines = IOUtils.readLines(new StringReader(sqlQuery));

			StringBuilder hintsStr = new StringBuilder();
			for (String l : lines) {
				l = l.trim();
				if (l.startsWith("--")) {
					hintsStr.append(l.substring(2).trim() + LINE_SEPARATOR);
				}
			}

			props.load(new StringReader(hintsStr.toString()));

		}
		catch (IOException e) {
			throw new ScriptingException(e);
		}

		return props;
	}

	private String _getHint(
		String hintName, Map<String, String> hints, String defaultValue) {

		String hint = null;

		if (hints.containsKey(hintName)) {
			hint = hints.get(hintName);
		}
		else {
			hint = defaultValue;
		}

		return hint;
	}

	private int _getIntegerHint(
		String hintName, Map<String, String> hints, int defaultValue)
		throws ScriptingException {

		int hint;

		if (hints.containsKey(hintName)) {
			try {
				hint = Integer.parseInt(hints.get(hintName));
			}
			catch (NumberFormatException nfe) {
				throw new ScriptingException("Value for " + hintName +
					" hint must be an integer", nfe);
			}
		}
		else {
			hint = defaultValue;
		}

		return hint;
	}

	private void _formatCSV(
		List<String> columnLabels, List<List<Object>> rows,
		UnsyncPrintWriter out) {

		List<List<?>> allLines = new LinkedList();
		allLines.add(columnLabels);
		allLines.addAll(rows);

		for (List<?> line : allLines) {
			for (Iterator<?> iterator = line.iterator(); iterator.hasNext();) {
				Object value = (Object) iterator.next();
				if (value != null) {
					
					out.append("\"" + value.toString().replaceAll("\"", "\"\"") + "\"");
				} else {
					out.append("\"null\"");
				}
				if (iterator.hasNext()) {
					out.append(',');
				}
			}
			out.append("\r\n");
		}
	}

	private void _displayHelp(UnsyncPrintWriter out) {
		out.append("Usage:\nTo set execution parameters, you can add some hint lines in your script, like these:\n");
		out.append("\t--help\t\t\tDisplay this help\n");
		out.append("\t--format=html\t\tFormat results in HTML (default format)\n");
		out.append("\t--format=csv\t\tFormat results in CSV\n");
		out.append("\t--maxRows=200\t\tLimit display to 200 first results (default limit is 50)\n");
		out.append("You can combine several hints using multiple lines.\n\n");
		out.append("Learn more about SQL Query Scripting Hook at <a href=\"https://github.com/slemarchand/sql-query-scripting-hook/wiki\" target=\"_blank\">https://github.com/slemarchand/sql-query-scripting-hook/wiki</a>.\n\n");
	}
	
	private void _formatHTML(
		List<String> columnLabels, List<List<Object>> rows,
		UnsyncPrintWriter out) {

		out.append("</pre>");
		out.append("<div class=\"component searchcontainer\"><div class=\"searchcontainer-content\">");
		out.append("<table class=\"table table-bordered table-striped\">");
		out.append("<thead class=\"table-columns\">");
		out.append("<tr>");

		for (String value : columnLabels) {
			out.append("<th>");
			out.append(value);
			out.append("</th>");
		}

		out.append("</tr>");
		out.append("</thead>");
		out.append("<tbody class=\"table-data\">");

		boolean alt = false;
		for (List<?> line : rows) {
			out.append("<tr class=\"portlet-section-alternate results-row " +
				(alt ? "alt" : "") + "\">");
			for (int i=0; i < line.size(); i++) {
				Object value = (Object) line.get(i);
				
				out.append("<td class=\"table-cell");
				if(i==0) {
					out.append(" first");
				} else if(i==line.size()) {
					out.append(" last");
				}
				out.append("\">");
				if (value != null) {
					out.append(value.toString());
				} else {
					out.append("<span style=\"font-style:italic\">null</span>");
				}
				out.append("</td>");
			}
			out.append("</tr>");
			alt = !alt;
		}

		out.append("</tbody>");

		out.append("</table>");
		out.append("</div></div>");
		out.append("</pre>");
	}

	private List<List<Object>> _execQuery(
		String sqlQuery, int maxRows, List<String> columnLabels)
		throws SQLException {

		List<List<Object>> rows = null;

		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection();
			
			con.setAutoCommit(false); // Prevent data updates
			
			stmt = con.createStatement();
			stmt.setMaxRows(maxRows);
			rs = stmt.executeQuery(sqlQuery);

			ResultSetMetaData md = rs.getMetaData();
			int cc = md.getColumnCount();

			rows = new ArrayList<List<Object>>(cc);

			columnLabels.clear();

			for (int c = 1; c <= cc; c++) {
				String cl = md.getColumnLabel(c);
				columnLabels.add(cl);
			}

			while (rs.next()) {
				List<Object> row = new ArrayList<Object>(cc);
				for (int c = 1; c <= cc; c++) {
					Object value = rs.getObject(c);
					row.add(value);
				}
				rows.add(row);
			}

		}
		finally {
			DataAccess.cleanUp(con, stmt, rs);
		}

		return rows;
	}

	private static String LINE_SEPARATOR = System.getProperty("line.separator");

}

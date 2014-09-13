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

package com.slemarchand.sqlqueryscripting.hook.events;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.scripting.Scripting;
import com.liferay.portal.kernel.scripting.ScriptingException;
import com.liferay.portal.kernel.scripting.ScriptingExecutor;
import com.liferay.portal.kernel.scripting.ScriptingUtil;
import com.slemarchand.sqlqueryscripting.scripting.sqlquery.SQLQueryExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Sébastien Le Marchand
 */
public class AppStartupAction extends SimpleAction {


	/*
	 * (non-System-doc)
	 * @see com.liferay.portal.kernel.events.SimpleAction#run(String[] ids)
	 */
	public void run(String[] ids)
		throws ActionException {

		if (_log.isInfoEnabled()) {
			_log.info("Adding scripting executor for database queries - http://photons-project.org/sqlqueryscripting");
		}

		Scripting scripting = ScriptingUtil.getScripting();
		try {
		
			addScriptingExecutor(scripting,
					SQLQueryExecutor.LANGUAGE, new SQLQueryExecutor());

		
			scripting.clearCache(SQLQueryExecutor.LANGUAGE);
		}
		catch (ScriptingException e) {
			throw new ActionException(e);
		}

	}

	private void addScriptingExecutor(Scripting scripting, String language,
			ScriptingExecutor scriptingExecutor) throws ScriptingException {
		
		
			Method method;
			try {
				method = scripting.getClass().getMethod(
									"addScriptingExecutor",
									String.class, ScriptingExecutor.class);
			} catch (SecurityException e) {
				throw new ScriptingException(e);
			} catch (NoSuchMethodException e) {
				throw new ScriptingException(e);
			}
			try {
				method.invoke(scripting, language, scriptingExecutor);
			} catch (IllegalArgumentException e) {
				throw new ScriptingException(e);
			} catch (IllegalAccessException e) {
				throw new ScriptingException(e);
			} catch (InvocationTargetException e) {
				throw new ScriptingException(e);
			}		
	}

	private final static Log _log =
		LogFactoryUtil.getLog(AppStartupAction.class);

}

/**
 * Copyright (c) 2012-2013 Sébastien Le Marchand, All rights reserved.
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
		
			addScriptionExecutor(scripting,
					SQLQueryExecutor.LANGUAGE, new SQLQueryExecutor());

		
			scripting.clearCache(SQLQueryExecutor.LANGUAGE);
		}
		catch (ScriptingException e) {
			throw new ActionException(e);
		}

	}

	private void addScriptionExecutor(Scripting scripting, String language,
			SQLQueryExecutor sqlQueryExecutor) throws ScriptingException {
		
		try {
			Method method = scripting.getClass().getMethod(
								"addScriptingExecutor",
								String.class, ScriptingExecutor.class);
			method.invoke(scripting, language, sqlQueryExecutor);
		} catch(ReflectiveOperationException e) {
			throw new ScriptingException(e);
		}
	}

	private final static Log _log =
		LogFactoryUtil.getLog(AppStartupAction.class);

}
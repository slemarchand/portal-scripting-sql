/**
 * Copyright (c) 2012 Sébastien Le Marchand, All rights reserved.
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

package photons.sqlqueryscripting.hook.events;

import photons.sqlqueryscripting.scripting.sqlquery.SQLQueryExecutor;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.scripting.Scripting;
import com.liferay.portal.kernel.scripting.ScriptingException;
import com.liferay.portal.kernel.scripting.ScriptingUtil;

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

		scripting.addScriptionExecutor(
			SQLQueryExecutor.LANGUAGE, new SQLQueryExecutor());

		try {
			scripting.clearCache(SQLQueryExecutor.LANGUAGE);
		}
		catch (ScriptingException e) {
			throw new ActionException(e);
		}

	}

	private final static Log _log =
		LogFactoryUtil.getLog(AppStartupAction.class);

}

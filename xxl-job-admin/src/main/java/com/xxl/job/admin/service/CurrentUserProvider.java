package com.xxl.job.admin.service;

import com.xxl.sso.core.model.LoginInfo;
import jakarta.servlet.http.HttpServletRequest;

public interface CurrentUserProvider {

	/**
	 * Business user id used for lobster task ownership isolation.
	 */
	long currentBusinessUserId(HttpServletRequest request);

	/**
	 * Resolve business user id with request body userId taking precedence when supplied.
	 */
	long currentBusinessUserId(HttpServletRequest request, Long requestUserId);

	/**
	 * System identity used only for native XXL-JOB job operations.
	 */
	LoginInfo schedulerLoginInfo();
}

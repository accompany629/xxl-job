package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.service.CurrentUserProvider;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BusinessCurrentUserProvider implements CurrentUserProvider {

	private static final Pattern JWT_USER_ID_PATTERN = Pattern.compile("\"(?:userId|user_id|sub)\"\\s*:\\s*\"?(\\d+)\"?");

	@Value("${lobster.schedule.operator-user-id:0}")
	private String operatorUserId;
	@Value("${lobster.schedule.operator-username:lobster-schedule}")
	private String operatorUsername;

	@Override
	public long currentBusinessUserId(HttpServletRequest request) {
		return currentBusinessUserId(request, null);
	}

	@Override
	public long currentBusinessUserId(HttpServletRequest request, Long requestUserId) {
		if (requestUserId != null && requestUserId > 0) {
			return requestUserId;
		}

		String userId = firstNotBlank(
				request.getHeader("X-Business-User-Id"),
				request.getHeader("X-User-Id"),
				request.getParameter("businessUserId"),
				request.getParameter("userId"),
				parseBearerJwtUserId(request.getHeader("Authorization"))
		);
		if (StringTool.isBlank(userId) || !StringTool.isNumeric(userId)) {
			throw new IllegalStateException("business userId required");
		}
		return Long.parseLong(userId);
	}

	@Override
	public LoginInfo schedulerLoginInfo() {
		LoginInfo loginInfo = new LoginInfo(operatorUserId, "lobster-schedule-system");
		loginInfo.setUserName(operatorUsername);
		loginInfo.setRoleList(List.of(Consts.ADMIN_ROLE));
		return loginInfo;
	}

	private String firstNotBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringTool.isNotBlank(value)) {
				return value.trim();
			}
		}
		return null;
	}

	private String parseBearerJwtUserId(String authorization) {
		if (StringTool.isBlank(authorization) || !authorization.startsWith("Bearer ")) {
			return null;
		}
		String token = authorization.substring("Bearer ".length()).trim();
		String[] parts = token.split("\\.");
		if (parts.length < 2) {
			return null;
		}
		try {
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			Matcher matcher = JWT_USER_ID_PATTERN.matcher(payload);
			return matcher.find() ? matcher.group(1) : null;
		} catch (Exception e) {
			return null;
		}
	}
}

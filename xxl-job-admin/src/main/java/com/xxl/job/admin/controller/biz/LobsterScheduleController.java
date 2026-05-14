package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.model.dto.lobster.CreateLobsterScheduleRequest;
import com.xxl.job.admin.model.dto.lobster.LobsterScheduleTaskVO;
import com.xxl.job.admin.model.dto.lobster.UpdateLobsterScheduleRequest;
import com.xxl.job.admin.service.CurrentUserProvider;
import com.xxl.job.admin.service.LobsterScheduleTaskService;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/api/lobster-schedules")
public class LobsterScheduleController {
	private static final Logger logger = LoggerFactory.getLogger(LobsterScheduleController.class);

	@Resource
	private LobsterScheduleTaskService lobsterScheduleTaskService;
	@Resource
	private CurrentUserProvider currentUserProvider;

	@PostMapping
	@ResponseBody
	public Response<LobsterScheduleTaskVO> create(HttpServletRequest servletRequest,
												  @RequestBody CreateLobsterScheduleRequest request) {
		try {
			LoginInfo loginInfo = currentUserProvider.schedulerLoginInfo();
			long userId = currentUserProvider.currentBusinessUserId(servletRequest, request == null ? null : request.getUserId());
			return lobsterScheduleTaskService.createTask(userId, loginInfo, request);
		} catch (Exception e) {
			logger.error("lobster schedule create error", e);
			return Response.ofFail(e.getMessage());
		}
	}

	@GetMapping
	@ResponseBody
	public Response<List<LobsterScheduleTaskVO>> list(HttpServletRequest servletRequest) {
		try {
			long userId = currentUserProvider.currentBusinessUserId(servletRequest);
			return lobsterScheduleTaskService.listByUserId(userId);
		} catch (Exception e) {
			logger.error("lobster schedule list error", e);
			return Response.ofFail(e.getMessage());
		}
	}

	@GetMapping("/{id}")
	@ResponseBody
	public Response<LobsterScheduleTaskVO> get(HttpServletRequest servletRequest, @PathVariable long id) {
		try {
			long userId = currentUserProvider.currentBusinessUserId(servletRequest);
			return lobsterScheduleTaskService.getByIdAndUserId(id, userId);
		} catch (Exception e) {
			logger.error("lobster schedule get error, id={}", id, e);
			return Response.ofFail(e.getMessage());
		}
	}

	@PutMapping("/{id}")
	@ResponseBody
	public Response<LobsterScheduleTaskVO> update(HttpServletRequest servletRequest,
												  @PathVariable long id,
												  @RequestBody UpdateLobsterScheduleRequest request) {
		try {
			LoginInfo loginInfo = currentUserProvider.schedulerLoginInfo();
			long userId = currentUserProvider.currentBusinessUserId(servletRequest, request == null ? null : request.getUserId());
			return lobsterScheduleTaskService.updateTask(id, userId, loginInfo, request);
		} catch (Exception e) {
			logger.error("lobster schedule update error, id={}", id, e);
			return Response.ofFail(e.getMessage());
		}
	}

	@PostMapping("/{id}/enable")
	@ResponseBody
	public Response<String> enable(HttpServletRequest servletRequest, @PathVariable long id) {
		try {
			LoginInfo loginInfo = currentUserProvider.schedulerLoginInfo();
			long userId = currentUserProvider.currentBusinessUserId(servletRequest);
			return lobsterScheduleTaskService.enableTask(id, userId, loginInfo);
		} catch (Exception e) {
			logger.error("lobster schedule enable error, id={}", id, e);
			return Response.ofFail(e.getMessage());
		}
	}

	@PostMapping("/{id}/disable")
	@ResponseBody
	public Response<String> disable(HttpServletRequest servletRequest, @PathVariable long id) {
		try {
			LoginInfo loginInfo = currentUserProvider.schedulerLoginInfo();
			long userId = currentUserProvider.currentBusinessUserId(servletRequest);
			return lobsterScheduleTaskService.disableTask(id, userId, loginInfo);
		} catch (Exception e) {
			logger.error("lobster schedule disable error, id={}", id, e);
			return Response.ofFail(e.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public Response<String> delete(HttpServletRequest servletRequest, @PathVariable long id) {
		try {
			LoginInfo loginInfo = currentUserProvider.schedulerLoginInfo();
			long userId = currentUserProvider.currentBusinessUserId(servletRequest);
			return lobsterScheduleTaskService.deleteTaskSoft(id, userId, loginInfo);
		} catch (Exception e) {
			logger.error("lobster schedule delete error, id={}", id, e);
			return Response.ofFail(e.getMessage());
		}
	}
}

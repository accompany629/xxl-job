package com.xxl.job.admin.service;

import com.xxl.job.admin.model.dto.lobster.CreateLobsterScheduleRequest;
import com.xxl.job.admin.model.dto.lobster.LobsterScheduleTaskVO;
import com.xxl.job.admin.model.dto.lobster.UpdateLobsterScheduleRequest;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;

import java.util.List;

public interface LobsterScheduleTaskService {

	Response<LobsterScheduleTaskVO> createTask(long userId, LoginInfo loginInfo, CreateLobsterScheduleRequest request);

	Response<List<LobsterScheduleTaskVO>> listByUserId(long userId);

	Response<LobsterScheduleTaskVO> getByIdAndUserId(long id, long userId);

	Response<LobsterScheduleTaskVO> updateTask(long id, long userId, LoginInfo loginInfo, UpdateLobsterScheduleRequest request);

	Response<String> enableTask(long id, long userId, LoginInfo loginInfo);

	Response<String> disableTask(long id, long userId, LoginInfo loginInfo);

	Response<String> deleteTaskSoft(long id, long userId, LoginInfo loginInfo);

	Response<String> updateLastExecuteResult(long id, String status, String message);
}

package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.LobsterScheduleTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LobsterScheduleTaskMapper {

	int save(LobsterScheduleTask task);

	List<LobsterScheduleTask> listByUserId(@Param("userId") long userId);

	LobsterScheduleTask loadById(@Param("id") long id);

	LobsterScheduleTask loadByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

	int update(LobsterScheduleTask task);

	int updateXxlJobId(@Param("id") long id, @Param("xxlJobId") int xxlJobId);

	int updateStatus(@Param("id") long id, @Param("userId") long userId, @Param("status") int status);

	int softDelete(@Param("id") long id, @Param("userId") long userId);

	int updateLastExecuteResult(@Param("id") long id,
								@Param("lastExecuteStatus") String lastExecuteStatus,
								@Param("lastExecuteMsg") String lastExecuteMsg);
}

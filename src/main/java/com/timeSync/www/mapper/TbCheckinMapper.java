package com.timeSync.www.mapper;

import com.timeSync.www.dto.MonthCountForm;
import com.timeSync.www.entity.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author fishx
 * @description 针对表【tb_checkin(签到表)】的数据库操作Mapper
 * @createDate 2023-08-23 12:03:43
 * @Entity com.timeSync.www.entity.TbCheckin
 */
@Mapper
public interface TbCheckinMapper {

    public Integer haveCheckin(HashMap param);

    public void insert(TbCheckin checkin);

    public HashMap searchTodayCheckin(int userId);

    public long searchCheckinDays(int userId);

    public ArrayList<HashMap> searchWeekCheckin(HashMap param);

    public List<HashMap> signInManagement(HashMap param);

    public int selectNormalCount(String status);

    public List<MonthCountForm> getAllMonthCount();

}





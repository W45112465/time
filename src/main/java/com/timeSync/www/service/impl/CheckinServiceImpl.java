package com.timeSync.www.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.hash.Hash;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timeSync.www.dto.AttendanceResultsForm;
import com.timeSync.www.dto.CardInfoForm;
import com.timeSync.www.entity.SystemConstants;
import com.timeSync.www.entity.TbCheckin;
import com.timeSync.www.entity.TbFaceModel;
import com.timeSync.www.exception.ConditionException;
import com.timeSync.www.mapper.*;
import com.timeSync.www.service.CheckinService;
import com.timeSync.www.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SystemConstants constants;

    @Autowired
    private TbHolidaysMapper holidaysMapper;

    @Autowired
    private TbWorkdayMapper workdayMapper;

    @Autowired
    private TbCheckinMapper checkinMapper;

    @Autowired
    private TbFaceModelMapper faceModelMapper;

    @Autowired
    private TbCityMapper cityMapper;

    @Autowired
    private TbUserMapper userMapper;

    @Autowired
    private TbDeptMapper deptMapper;

    @Value("${time-sync.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${time-sync.face.checkinUrl}")
    private String checkinUrl;

    @Value("${time-sync.code}")
    private String code;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = holidaysMapper.searchTodayIsHolidays() != null ? true : false;
        boolean bool_2 = workdayMapper.searchTodayWorkday() != null ? true : false;
        String type = "工作日";
        if (DateUtil.date().isWeekend()) {
            type = "节假日";
        }
        if (bool_1) {
            type = "节假日";
        }
        if (bool_2) {
            type = "工作日";
        }
        if (type.equals("节假日")) {
            return "节假日不需要考勤";
        } else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "没到上班考勤开始时间";
            } else if (now.isAfter(attendanceEnd)) {
                return "超过了上班考勤结束时间";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                boolean bool = checkinMapper.haveCheckin(map) != null ? true : false;
                return bool ? "今日已经考勤，不用重复考勤" : "可以考勤";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {
        //判断签到
        Date d1 = DateUtil.date();//当前时间
        Date d2 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceTime);//开始时间
        Date d3 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);//结束时间
        int status = 1;
        if (d1.compareTo(d2) <= 0) {
            status = 1;//正常签到
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            status = 2;//迟到
        }
        //查询签到人的人脸模型数据
        int userId = (Integer) param.get("userId");
        String faceModel = faceModelMapper.searchFaceModel(userId);
        if (faceModel == null) {
            throw new ConditionException("不存在人脸模型");
        } else {
            String path = (String) param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path), "targetModel", faceModel);
            request.form("code", code);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                throw new ConditionException("人脸识别服务异常");
            }
            String body = response.body();
            if ("无法识别出人脸".equals(body) || "照片存在多张人脸".equals(body)) {
                throw new ConditionException(body);
            } else if ("False".equals(body)) {
                throw new ConditionException("签到无效，非本人签到");
            } else if ("True".equals(body)) {
                //TODO 判断所在区域是否在打卡区域
                String latitude = (String) param.get("latitude"); //纬度
                String longitude = (String) param.get("dimensionality"); //经度
                if (!StrUtil.isBlank(latitude) && !StrUtil.isBlank(longitude)) {
                    try {
                        //获取所在地是否在公司
                        String url = "https://apis.map.qq.com/ws/place/v1/search?boundary=nearby(" + latitude + "," + longitude + ",10)&keyword=北大青鸟(实力校区)&key=HXYBZ-VRQ3B-JW7UP-JICIM-AXJB7-J7B4R";
                        String resp = HttpUtil.get(url);
                        JSONObject jsonObject = JSONUtil.parseObj(resp);
                        String statu = jsonObject.getStr("status");
                        if ("0".equals(status)) {
                            JSONArray jsonArray = JSONUtil.parseArray(jsonObject.getStr("data"));
                            System.out.println(jsonArray);
                            JSONObject jsonArrayJSONObject = jsonArray.getJSONObject(0);
                            String title = jsonArrayJSONObject.getStr("title");
                            if (!("北大青鸟(实力校区)".equals(title))) {
                                throw new ConditionException("签到失败,请在公司区域签到");
                            }
                        }
                    } catch (Exception e) {
                        throw new ConditionException("获取所在位置失败");
                    }
                }
                //TODO 保存签到记录
                String address = (String) param.get("address");
                String country = (String) param.get("country");
                String province = (String) param.get("province");
                String city = (String) param.get("city");
                String district = (String) param.get("district");
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus(status);
                entity.setRisk(1);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinMapper.insert(entity);
            }
        }
    }

    @Override
    public void createFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        request.form("photo", FileUtil.file(path));
        request.form("code", code);
        HttpResponse response = request.execute();
        String body = response.body();
        if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".endsWith(body)) {
            throw new ConditionException(body);
        } else {
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelMapper.insert(entity);
        }
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map = checkinMapper.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinMapper.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList = checkinMapper.searchWeekCheckin(param);
        ArrayList holidaysList = holidaysMapper.searchHolidaysInRange(param);
        ArrayList workdayList = workdayMapper.searchWorkdayInRange(param);
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        ArrayList<HashMap> list = new ArrayList<>();
        range.forEach(one -> {
            String date = one.toString("yyyy-MM-dd");
            String type = "工作日";
            if (one.isWeekend()) {
                type = "节假日";
            }
            if (holidaysList != null && holidaysList.contains(date)) {
                type = "节假日";
            } else if (workdayList != null && workdayList.contains(date)) {
                type = "工作日";
            }
            String status = "";
            if (type.equals("工作日") && DateUtil.compare(one, DateUtil.date()) <= 0) {
                status = "缺勤";
                boolean flag = false;
                for (HashMap<String, String> map : checkinList) {
                    if (map.containsValue(date)) {
                        status = map.get("status");
                        flag = true;
                        break;
                    }
                }
                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
                String today = DateUtil.today();
                if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag == false) {
                    status = "";
                }
            }
            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("周"));
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }

    @Override
    public PageInfo<HashMap> signInManagement(HashMap param) {
        HashMap map = new HashMap();
        map.put("userId", 0);
        map.put("status", 0);
        if (param.get("status") != null && !param.get("status").equals("")) {
            map.put("status", Integer.parseInt(param.get("status").toString()));
        }
        if (param.get("userId") != null && !param.get("userId").equals("")) {
            map.put("userId", Integer.parseInt(param.get("userId").toString()));
        }
        List<HashMap> checkInMap = checkinMapper.signInManagement(map);
        PageHelper.startPage(Integer.parseInt(param.get("offset").toString()), Integer.parseInt(param.get("size").toString()));
        return new PageInfo<>(checkInMap);
    }

    @Override
    public List<CardInfoForm> searchUserCount() {
        ArrayList<CardInfoForm> list = new ArrayList<>();
        int all = userMapper.selectUserCount();
        int normal = checkinMapper.selectNormalCount(1 + "");
        int beLate = checkinMapper.selectNormalCount(2 + "");
        int absenteeism = all - normal - beLate;
        list.add(new CardInfoForm("员工人数", "员工人数", new BigDecimal(all), "公司总人数:"));
        list.add(new CardInfoForm("正常签到数", "正常签到人数", new BigDecimal(normal), "今日正常签到人数:"));
        list.add(new CardInfoForm("迟到数", "迟到人数", new BigDecimal(beLate), "今日迟到人数:"));
        list.add(new CardInfoForm("缺勤数", "缺勤人数", new BigDecimal(absenteeism), "今日缺勤人数:"));
        return list;
    }

    public List<AttendanceResultsForm> getAttendanceResultCount() {
        ArrayList<AttendanceResultsForm> list = new ArrayList<>();
        int all = userMapper.selectUserCount();
        int normal = checkinMapper.selectNormalCount(1 + "");
        int beLate = checkinMapper.selectNormalCount(2 + "");
        int absenteeism = all - normal - beLate;
        list.add(new AttendanceResultsForm("总人数", all));
        list.add(new AttendanceResultsForm("正常签到", normal));
        list.add(new AttendanceResultsForm("迟到数", beLate));
        list.add(new AttendanceResultsForm("缺勤数", absenteeism));
        return list;
    }

    @Override
    public R other() {
        Map<String, Object> map = new HashMap<>();
        map.put("cardInfo", searchUserCount());
        map.put("attendanceResults", getAttendanceResultCount());
        List<String> months = new ArrayList<>();
        List<Integer> dataList = new ArrayList<>();
        checkinMapper.getAllMonthCount().forEach(monthCountDTO -> {
            months.add(monthCountDTO.getMonth() + "月");
            dataList.add(monthCountDTO.getCount());
        });
        Map<String, Object> lineInfo = new HashMap<>();
        lineInfo.put("xAxis", months);
        lineInfo.put("yAxis", dataList);
        map.put("lineInfo", lineInfo);
        return R.ok().put("data",map);
    }
}

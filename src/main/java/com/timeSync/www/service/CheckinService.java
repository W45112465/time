package com.timeSync.www.service;

import com.github.pagehelper.PageInfo;
import com.timeSync.www.dto.CardInfoForm;
import com.timeSync.www.utils.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface CheckinService {
    public String validCanCheckIn(int userId,String date);
    public void checkin(HashMap param);
    public void createFaceModel(int userId,String path);
    public HashMap searchTodayCheckin(int userId);
    public long searchCheckinDays(int userId);
    public ArrayList<HashMap> searchWeekCheckin(HashMap param);
    public ArrayList<HashMap> searchMonthCheckin(HashMap param);
    public PageInfo<HashMap>  signInManagement(HashMap param);
    public List<CardInfoForm> searchUserCount();
    public R other();
}

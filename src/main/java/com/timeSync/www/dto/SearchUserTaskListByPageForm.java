package com.timeSync.www.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel
@Data
public class SearchUserTaskListByPageForm {
    private String current;
}

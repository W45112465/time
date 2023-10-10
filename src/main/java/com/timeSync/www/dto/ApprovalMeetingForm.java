package com.timeSync.www.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class ApprovalMeetingForm {
    @NotNull
    private String id;
    @NotNull
    private String approval;
}

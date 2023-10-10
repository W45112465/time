package com.timeSync.www.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class SearchRoomIdByUUIDForm {
    @NotBlank
    private String uuid;
}

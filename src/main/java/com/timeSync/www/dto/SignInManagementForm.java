package com.timeSync.www.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class SignInManagementForm {
    @NotNull
    @Min(1)
    private int offset;
    @NotNull
    @Min(2)
    private int size;
    private String userId;
    private String status;
}

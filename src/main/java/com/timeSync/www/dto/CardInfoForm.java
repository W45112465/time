package com.timeSync.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardInfoForm {
    private String tip;
    private String title;
    private BigDecimal number;
    private String subtitle;
}

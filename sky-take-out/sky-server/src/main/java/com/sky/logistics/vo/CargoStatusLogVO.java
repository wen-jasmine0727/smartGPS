package com.sky.logistics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoStatusLogVO {

    private String id;

    private String cargoId;

    private String status;

    private BigDecimal lat;

    private BigDecimal lng;

    private String remark;

    private String operatorId;

    private OffsetDateTime createdAt;
}

package com.sky.logistics.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("更新货物状态请求")
public class CargoStatusUpdateDTO {

    @ApiModelProperty(value = "货物状态", example = "IN_TRANSIT")
    private String status;

    @ApiModelProperty(value = "纬度", example = "31.2304")
    private BigDecimal lat;

    @ApiModelProperty(value = "经度", example = "121.4737")
    private BigDecimal lng;

    @ApiModelProperty(value = "备注", example = "车辆已离开上海仓储中心")
    private String remark;

    @ApiModelProperty(value = "操作人 ID", example = "USR-003")
    private String operatorId;
}
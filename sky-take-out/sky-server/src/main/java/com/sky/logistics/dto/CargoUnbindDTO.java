package com.sky.logistics.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("解绑货物与车辆请求")
public class CargoUnbindDTO {

    @ApiModelProperty(value = "货物 ID", example = "SH-HZ-20260629-0291")
    private String cargoId;
}
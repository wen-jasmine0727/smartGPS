package com.sky.logistics.vo;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class VehicleVO {
    private Long id;
    private String plate;
    private String vinTopic;
    private String vehicleType;
    private Integer capacity;
    private String driverName;
    private String driverPhone;
    private String deviceImei;
    private String status;
    private String deviceStatus;
    private OffsetDateTime updatedAt;

}

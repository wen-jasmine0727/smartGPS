package com.sky.logistics.dto;

import lombok.Data;

@Data
public class VehicleUpdateDTO {
    private String plate;
    private String vehicleType;
    private Integer capacity;
    private String driverName;
    private String driverPhone;
    private String deviceImei;
    private String status;
    private String deviceStatus;
}

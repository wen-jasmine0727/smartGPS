package com.sky.logistics.service;

import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.VehicleCreateDTO;
import com.sky.logistics.dto.VehicleQueryDTO;
import com.sky.logistics.vo.VehicleVO;

public interface VehicleService {

    PageResponse<VehicleVO> page(VehicleQueryDTO queryDTO);

    VehicleVO detail(String plate);
    VehicleVO create(VehicleCreateDTO createDTO);
}

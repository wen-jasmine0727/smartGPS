package com.sky.logistics.service;

import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.CargoBindDTO;
import com.sky.logistics.dto.CargoCreateDTO;
import com.sky.logistics.dto.CargoQueryDTO;
import com.sky.logistics.dto.CargoUnbindDTO;
import com.sky.logistics.vo.CargoVO;

public interface CargoService {

    PageResponse<CargoVO> page(CargoQueryDTO queryDTO);

    CargoVO detail(String cargoId);

    CargoVO create(CargoCreateDTO createDTO);

    CargoVO bind(CargoBindDTO bindDTO);

    CargoVO unbind(CargoUnbindDTO unbindDTO);

}

package com.sky.logistics.service;

import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.*;
import com.sky.logistics.vo.CargoStatusLogVO;
import com.sky.logistics.vo.CargoVO;

import java.util.List;

public interface CargoService {

    PageResponse<CargoVO> page(CargoQueryDTO queryDTO);

    CargoVO detail(String cargoId);

    CargoVO create(CargoCreateDTO createDTO);

    CargoVO bind(CargoBindDTO bindDTO);

    CargoVO unbind(CargoUnbindDTO unbindDTO);

    CargoVO updateStatus(String CargoID,CargoStatusUpdateDTO updateDTO);

    List<CargoStatusLogVO> getStatusLogs(String cargoId);
}

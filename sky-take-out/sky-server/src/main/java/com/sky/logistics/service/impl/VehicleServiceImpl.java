package com.sky.logistics.service.impl;

import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.VehicleCreateDTO;
import com.sky.logistics.dto.VehicleQueryDTO;
import com.sky.logistics.entity.Vehicle;
import com.sky.logistics.mapper.LogisticsVehicleMapper;
import com.sky.logistics.service.VehicleService;
import com.sky.logistics.vo.VehicleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final LogisticsVehicleMapper vehicleMapper;

    public VehicleServiceImpl(LogisticsVehicleMapper vehicleMapper) {
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public PageResponse<VehicleVO> page(VehicleQueryDTO queryDTO) {
        int page = normalizePage(queryDTO == null ? null : queryDTO.getPage());
        int size = normalizeSize(queryDTO == null ? null : queryDTO.getSize());
        int offset = (page - 1) * size;
        String status = trimToNull(queryDTO == null ? null : queryDTO.getStatus());
        String keyword = trimToNull(queryDTO == null ? null : queryDTO.getKeyword());

        Long total = vehicleMapper.count(status, keyword);
        if (total == null || total == 0) {
            return new PageResponse<>(Collections.<VehicleVO>emptyList(), page, size, 0L, 0);
        }

        List<Vehicle> vehicles = vehicleMapper.findPage(status, keyword, offset, size);
        List<VehicleVO> content = vehicles == null
                ? Collections.<VehicleVO>emptyList()
                : vehicles.stream().map(this::toVO).collect(Collectors.toList());
        int totalPages = (int) Math.ceil((double) total / size);

        return new PageResponse<>(content, page, size, total, totalPages);
    }

    @Override
    public VehicleVO detail(String plate) {
        if (!StringUtils.hasText(plate)) {
            throw new IllegalArgumentException("车牌号不能为空");
        }

        Vehicle vehicle = vehicleMapper.findByPlate(plate);
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }

        return toVO(vehicle);
    }

    @Override
    @Transactional
    public VehicleVO create(VehicleCreateDTO createDTO) {
        if (createDTO == null) {
            throw new IllegalArgumentException("车辆信息不能为空");
        }

        String plate = trimToNull(createDTO.getPlate());
        String deviceImei = trimToNull(createDTO.getDeviceImei());
        if (!StringUtils.hasText(plate)) {
            throw new IllegalArgumentException("车牌号不能为空");
        }
        if (!StringUtils.hasText(deviceImei)) {
            throw new IllegalArgumentException("设备 IMEI 不能为空");
        }
        if (vehicleMapper.findByPlate(plate) != null) {
            throw new IllegalArgumentException("车牌号已存在");
        }
        if (vehicleMapper.findByDeviceImei(deviceImei) != null) {
            throw new IllegalArgumentException("设备 IMEI 已存在");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(plate);
        vehicle.setVinTopic(toVinTopic(plate));
        vehicle.setVehicleType(trimToNull(createDTO.getVehicleType()));
        vehicle.setCapacity(createDTO.getCapacity());
        vehicle.setDriverName(trimToNull(createDTO.getDriverName()));
        vehicle.setDriverPhone(trimToNull(createDTO.getDriverPhone()));
        vehicle.setDeviceImei(deviceImei);
        vehicle.setStatus("OFFLINE");
        vehicle.setDeviceStatus("OFFLINE");

        vehicleMapper.insert(vehicle);
        return toVO(vehicleMapper.findByPlate(plate));
    }

    private VehicleVO toVO(Vehicle vehicle) {
        return VehicleVO.builder()
                .plate(vehicle.getPlate())
                .vinTopic(vehicle.getVinTopic())
                .vehicleType(vehicle.getVehicleType())
                .capacity(vehicle.getCapacity())
                .driverName(vehicle.getDriverName())
                .driverPhone(vehicle.getDriverPhone())
                .deviceImei(vehicle.getDeviceImei())
                .status(vehicle.getStatus())
                .deviceStatus(vehicle.getDeviceStatus())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toVinTopic(String plate) {
        return plate.replace("·", "-");
    }
}

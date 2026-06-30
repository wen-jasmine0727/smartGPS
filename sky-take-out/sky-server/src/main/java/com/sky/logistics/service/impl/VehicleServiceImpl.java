package com.sky.logistics.service.impl;

import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.VehicleCreateDTO;
import com.sky.logistics.dto.VehicleQueryDTO;
import com.sky.logistics.dto.VehicleUpdateDTO;
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
    public VehicleVO detailById(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("车辆 ID 不正确");
        }

        Vehicle vehicle = vehicleMapper.findById(id);
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

    @Override
    @Transactional
    public VehicleVO update(Long id, VehicleUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new IllegalArgumentException("车辆信息不能为空");
        }

        Vehicle current = findRequiredById(id);
        String plate = trimToNull(updateDTO.getPlate());
        String deviceImei = trimToNull(updateDTO.getDeviceImei());

        if (!StringUtils.hasText(plate)) {
            plate = current.getPlate();
        }
        if (!StringUtils.hasText(deviceImei)) {
            deviceImei = current.getDeviceImei();
        }

        if (!plate.equals(current.getPlate())) {
            ensureNoBinding(id, "车辆存在绑定记录，不能修改车牌号");
            if (vehicleMapper.findByPlateExcludeId(plate, id) != null) {
                throw new IllegalArgumentException("车牌号已存在");
            }
        }
        if (!deviceImei.equals(current.getDeviceImei())
                && vehicleMapper.findByDeviceImeiExcludeId(deviceImei, id) != null) {
            throw new IllegalArgumentException("设备 IMEI 已存在");
        }

        current.setPlate(plate);
        current.setVinTopic(toVinTopic(plate));
        current.setVehicleType(valueOrCurrent(updateDTO.getVehicleType(), current.getVehicleType()));
        current.setCapacity(updateDTO.getCapacity() == null ? current.getCapacity() : updateDTO.getCapacity());
        current.setDriverName(valueOrCurrent(updateDTO.getDriverName(), current.getDriverName()));
        current.setDriverPhone(valueOrCurrent(updateDTO.getDriverPhone(), current.getDriverPhone()));
        current.setDeviceImei(deviceImei);
        current.setStatus(valueOrCurrent(updateDTO.getStatus(), current.getStatus()));
        current.setDeviceStatus(valueOrCurrent(updateDTO.getDeviceStatus(), current.getDeviceStatus()));

        vehicleMapper.update(current);
        return toVO(vehicleMapper.findById(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findRequiredById(id);
        ensureNoBinding(id, "车辆存在绑定记录，不能删除");
        vehicleMapper.deleteById(id);
    }

    private VehicleVO toVO(Vehicle vehicle) {
        return VehicleVO.builder()
                .id(vehicle.getId())
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

    private Vehicle findRequiredById(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("车辆 ID 不正确");
        }
        Vehicle vehicle = vehicleMapper.findById(id);
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        return vehicle;
    }

    private void ensureNoBinding(Long id, String message) {
        Long bindingCount = vehicleMapper.countBindingById(id);
        if (bindingCount != null && bindingCount > 0) {
            throw new IllegalArgumentException(message);
        }
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

    private String valueOrCurrent(String value, String current) {
        String trimmed = trimToNull(value);
        return trimmed == null ? current : trimmed;
    }

    private String toVinTopic(String plate) {
        return plate.replace("·", "-");
    }
}

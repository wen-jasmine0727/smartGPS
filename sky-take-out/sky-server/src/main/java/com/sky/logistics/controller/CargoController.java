package com.sky.logistics.controller;

import com.sky.logistics.common.ApiResponse;
import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.*;
import com.sky.logistics.service.CargoService;
import com.sky.logistics.service.LogisticsStarterService;
import com.sky.logistics.vo.CargoStatusLogVO;
import com.sky.logistics.vo.CargoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cargo")
@Api(tags = "智慧物流-货物管理")
public class CargoController {

    private final LogisticsStarterService starterService;
    private final CargoService cargoService;

    public CargoController(LogisticsStarterService starterService, CargoService cargoService) {
        this.starterService = starterService;
        this.cargoService = cargoService;
    }

    @GetMapping
    @ApiOperation("获取货物列表")
    public ApiResponse<PageResponse<CargoVO>> list(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        CargoQueryDTO queryDTO = new CargoQueryDTO();
        queryDTO.setStatus(status);
        queryDTO.setKeyword(keyword);
        queryDTO.setPage(page);
        queryDTO.setSize(size);
        return ApiResponse.success(cargoService.page(queryDTO));
    }

    @PostMapping
    @ApiOperation("新增货物")
    public ApiResponse<CargoVO> create(@RequestBody CargoCreateDTO request) {
        return ApiResponse.success(cargoService.create(request));
    }

    @GetMapping("/{cargoId}")
    @ApiOperation("获取货物详情")
    public ApiResponse<CargoVO> detail(@PathVariable String cargoId) {
        return ApiResponse.success(cargoService.detail(cargoId));
    }

    @PostMapping("/bind")
    @ApiOperation("绑定货物与车辆")
    public ApiResponse<CargoVO> bind(@RequestBody CargoBindDTO request) {
        return ApiResponse.success(cargoService.bind(request));
    }

    @PostMapping("/unbind")
    @ApiOperation("解绑货物与车辆")
    public ApiResponse<CargoVO> unbind(@RequestBody CargoUnbindDTO request) {
        return ApiResponse.success(cargoService.unbind(request));
    }

    @PutMapping("/{cargoId}/status")
    @ApiOperation("更新货物状态")
    public ApiResponse<CargoVO> updateStatus(@PathVariable String cargoId, @RequestBody CargoStatusUpdateDTO request) {
        return ApiResponse.success(cargoService.updateStatus(cargoId, request));
    }

    @GetMapping("/{cargoId}/status-logs")
    @ApiOperation("获取货物状态日志")
    public ApiResponse<List<CargoStatusLogVO>> statusLogs(@PathVariable String cargoId) {
        return ApiResponse.success(cargoService.getStatusLogs(cargoId));
    }

    @GetMapping("/{cargoId}/position")
    @ApiOperation("获取货物当前位置")
    public ApiResponse<Map<String, Object>> position(@PathVariable String cargoId) {
        return ApiResponse.success(starterService.cargoPosition(cargoId));
    }

    @GetMapping("/{cargoId}/trajectory")
    @ApiOperation("获取货物轨迹")
    public ApiResponse<Map<String, Object>> trajectory(@PathVariable String cargoId) {
        return ApiResponse.success(starterService.cargoTrajectory(cargoId));
    }

    @GetMapping("/{cargoId}/eta")
    @ApiOperation("获取货物 ETA")
    public ApiResponse<Map<String, Object>> eta(@PathVariable String cargoId) {
        return ApiResponse.success(starterService.cargoEta(cargoId));
    }

    @GetMapping("/{cargoId}/timeline")
    @ApiOperation("获取货物时间线")
    public ApiResponse<Map<String, Object>> timeline(@PathVariable String cargoId) {
        return ApiResponse.success(starterService.cargoTimeline(cargoId));
    }
}

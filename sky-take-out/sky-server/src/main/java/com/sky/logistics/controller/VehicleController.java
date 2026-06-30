package com.sky.logistics.controller;

import com.sky.logistics.common.ApiResponse;
import com.sky.logistics.common.PageResponse;
import com.sky.logistics.dto.VehicleCreateDTO;
import com.sky.logistics.dto.VehicleQueryDTO;
import com.sky.logistics.service.LogisticsStarterService;
import com.sky.logistics.service.VehicleService;
import com.sky.logistics.vo.VehicleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
@Api(tags = "智慧物流-车辆管理")
public class VehicleController {

    private final LogisticsStarterService starterService;
    private final VehicleService vehicleService;

    public VehicleController(LogisticsStarterService starterService, VehicleService vehicleService) {
        this.starterService = starterService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    @ApiOperation("获取车辆列表")
    public ApiResponse<PageResponse<VehicleVO>> list(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        VehicleQueryDTO queryDTO = new VehicleQueryDTO();
        queryDTO.setStatus(status);
        queryDTO.setKeyword(keyword);
        queryDTO.setPage(page);
        queryDTO.setSize(size);
        return ApiResponse.success(vehicleService.page(queryDTO));
    }

    @GetMapping("/{plate}")
    @ApiOperation("获取车辆详情")
    public ApiResponse<VehicleVO> detail(@PathVariable String plate) {
        return ApiResponse.success(vehicleService.detail(plate));
    }

    @PostMapping
    @ApiOperation("新增车辆")
    public ApiResponse<VehicleVO> create(@RequestBody VehicleCreateDTO request) {
        return ApiResponse.success(vehicleService.create(request));
    }

    @PutMapping("/{plate}")
    @ApiOperation("修改车辆")
    public ApiResponse<Map<String, Object>> update(@PathVariable String plate, @RequestBody Map<String, Object> request) {
        if (request == null) {
            request = new LinkedHashMap<>();
        }
        request.put("plate", plate);
        return ApiResponse.success(request);
    }

    @DeleteMapping("/{plate}")
    @ApiOperation("删除车辆")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String plate) {
        return ApiResponse.success(starterService.activeVehicleTask(plate));
    }

    @GetMapping("/{plate}/active-task")
    @ApiOperation("获取车辆当前任务")
    public ApiResponse<Map<String, Object>> activeTask(@PathVariable String plate) {
        return ApiResponse.success(starterService.activeVehicleTask(plate));
    }

    @PostMapping("/{plate}/command")
    @ApiOperation("下发车辆调度指令")
    public ApiResponse<Map<String, Object>> createCommand(@PathVariable String plate, @RequestBody Map<String, Object> request) {
        return ApiResponse.success(starterService.createCommand(plate, request));
    }

    @GetMapping("/{plate}/command/{commandId}")
    @ApiOperation("获取调度指令详情")
    public ApiResponse<Map<String, Object>> commandDetail(@PathVariable String plate, @PathVariable String commandId) {
        return ApiResponse.success(starterService.commandDetail(plate, commandId));
    }

    @GetMapping("/{plate}/commands")
    @ApiOperation("获取车辆调度指令列表")
    public ApiResponse<PageResponse<Map<String, Object>>> commands(@PathVariable String plate,
                                                                   @RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size) {
        return ApiResponse.success(starterService.commands(plate, page, size));
    }
}

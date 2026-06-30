package com.sky.logistics.mapper;

import com.sky.logistics.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogisticsVehicleMapper {

    List<Vehicle> findPage(@Param("status") String status,
                           @Param("keyword") String keyword,
                           @Param("offset") Integer offset,
                           @Param("limit") Integer limit);

    Long count(@Param("status") String status,
               @Param("keyword") String keyword);

    Vehicle findById(@Param("id") Long id);

    Vehicle findByPlate(@Param("plate") String plate);

    Vehicle findByPlateExcludeId(@Param("plate") String plate,
                                 @Param("id") Long id);

    int insert(Vehicle vehicle);

    Vehicle findByDeviceImei(@Param("deviceImei") String deviceImei);

    Vehicle findByDeviceImeiExcludeId(@Param("deviceImei") String deviceImei,
                                      @Param("id") Long id);

    Long countBindingById(@Param("id") Long id);

    int update(Vehicle vehicle);

    int deleteById(@Param("id") Long id);
}

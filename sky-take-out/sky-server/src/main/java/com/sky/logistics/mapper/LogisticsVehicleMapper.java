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

    Vehicle findByPlate(@Param("plate") String plate);

    Vehicle findByVinTopic(@Param("vinTopic") String vinTopic);

    Vehicle findByImei(@Param("imei") String imei);
}

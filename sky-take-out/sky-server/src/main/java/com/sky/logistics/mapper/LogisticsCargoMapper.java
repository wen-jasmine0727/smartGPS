package com.sky.logistics.mapper;

import com.sky.logistics.entity.Cargo;
import com.sky.logistics.entity.CargoRecord;
import com.sky.logistics.entity.CargoVehicleBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogisticsCargoMapper {

    List<CargoRecord> findPage(@Param("status") String status,
                               @Param("keyword") String keyword,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    Long count(@Param("status") String status,
               @Param("keyword") String keyword);

    CargoRecord findByCargoId(@Param("cargoId") String cargoId);

    int insert(Cargo cargo);

    Long countActiveBindingByCargoId(@Param("cargoId") String cargoId);

    Long countActiveBindingByVehicleId(@Param("vehicleId") Long vehicleId);

    void insertBinding(CargoVehicleBinding binding);

    void updateCargoStatus(@Param("cargoId") String cargoId, @Param("status") String status);

    CargoVehicleBinding findActiveBindingByCargoId(@Param("cargoId") String cargoId);

    int unbindById(@Param("id") String id);
}

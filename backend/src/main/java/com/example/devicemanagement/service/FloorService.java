package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.request.FloorRequest;
import com.example.devicemanagement.entity.Floor;

import java.util.List;

public interface FloorService {

    Floor createFloor(FloorRequest request);

    Floor updateFloor(Long id, FloorRequest request);

    void deleteFloor(Long id);

    Floor getFloorById(Long id);

    List<Floor> getAllFloors();

    List<Floor> getFloorsByBuilding(String buildingName);
}

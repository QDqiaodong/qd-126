package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.request.FloorRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.service.FloorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/floor")
@CrossOrigin(origins = "*")
public class FloorController {

    @Autowired
    private FloorService floorService;

    @PostMapping
    public ApiResponse<Floor> createFloor(@RequestBody FloorRequest request) {
        Floor floor = floorService.createFloor(request);
        return ApiResponse.success(floor);
    }

    @PutMapping("/{id}")
    public ApiResponse<Floor> updateFloor(@PathVariable Long id, @RequestBody FloorRequest request) {
        Floor floor = floorService.updateFloor(id, request);
        return ApiResponse.success(floor);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFloor(@PathVariable Long id) {
        floorService.deleteFloor(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Floor> getFloorById(@PathVariable Long id) {
        Floor floor = floorService.getFloorById(id);
        return ApiResponse.success(floor);
    }

    @GetMapping
    public ApiResponse<List<Floor>> getAllFloors() {
        List<Floor> floors = floorService.getAllFloors();
        return ApiResponse.success(floors);
    }

    @GetMapping("/building/{buildingName}")
    public ApiResponse<List<Floor>> getFloorsByBuilding(@PathVariable String buildingName) {
        List<Floor> floors = floorService.getFloorsByBuilding(buildingName);
        return ApiResponse.success(floors);
    }
}

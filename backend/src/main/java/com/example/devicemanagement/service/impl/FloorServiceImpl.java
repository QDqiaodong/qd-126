package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.devicemanagement.dto.request.FloorRequest;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.mapper.FloorMapper;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FloorServiceImpl implements FloorService {

    @Autowired
    private FloorMapper floorMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    @Transactional
    public Floor createFloor(FloorRequest request) {
        Floor floor = new Floor();
        floor.setFloorName(request.getFloorName());
        floor.setFloorNumber(request.getFloorNumber());
        floor.setBuildingName(request.getBuildingName());
        floorMapper.insert(floor);
        redisCacheService.invalidateFloorCache();
        return floor;
    }

    @Override
    @Transactional
    public Floor updateFloor(Long id, FloorRequest request) {
        Floor floor = floorMapper.selectById(id);
        if (floor == null) {
            throw new RuntimeException("楼层不存在");
        }
        floor.setFloorName(request.getFloorName());
        floor.setFloorNumber(request.getFloorNumber());
        floor.setBuildingName(request.getBuildingName());
        floorMapper.updateById(floor);
        redisCacheService.invalidateFloorCache();
        redisCacheService.invalidateRoomCache(id);
        return floor;
    }

    @Override
    @Transactional
    public void deleteFloor(Long id) {
        Floor floor = floorMapper.selectById(id);
        if (floor == null) {
            throw new RuntimeException("楼层不存在");
        }
        floorMapper.deleteById(id);
        redisCacheService.invalidateFloorCache();
        redisCacheService.invalidateRoomCache(id);
    }

    @Override
    public Floor getFloorById(Long id) {
        return floorMapper.selectById(id);
    }

    @Override
    public List<Floor> getAllFloors() {
        try {
            Object cached = redisCacheService.getCachedFloorList();
            if (cached != null) {
                return (List<Floor>) cached;
            }
        } catch (Exception e) {
        }
        LambdaQueryWrapper<Floor> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Floor::getFloorNumber);
        List<Floor> floors = floorMapper.selectList(wrapper);
        try {
            redisCacheService.cacheFloorList(floors);
        } catch (Exception e) {
        }
        return floors;
    }

    @Override
    public List<Floor> getFloorsByBuilding(String buildingName) {
        LambdaQueryWrapper<Floor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Floor::getBuildingName, buildingName);
        wrapper.orderByAsc(Floor::getFloorNumber);
        return floorMapper.selectList(wrapper);
    }
}

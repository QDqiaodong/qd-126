package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.devicemanagement.dto.request.ReceptionRoomRequest;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.ReceptionRoomMapper;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReceptionRoomServiceImpl implements ReceptionRoomService {

    @Autowired
    private ReceptionRoomMapper roomMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    @Transactional
    public ReceptionRoom createRoom(ReceptionRoomRequest request) {
        ReceptionRoom room = new ReceptionRoom();
        room.setRoomName(request.getRoomName());
        room.setRoomCode(request.getRoomCode());
        room.setFloorId(request.getFloorId());
        room.setCapacity(request.getCapacity());
        room.setEquipmentCount(request.getEquipmentCount() != null ? request.getEquipmentCount() : 0);
        room.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        roomMapper.insert(room);
        try {
            redisCacheService.invalidateRoomCache(request.getFloorId());
        } catch (Exception e) {
        }
        return room;
    }

    @Override
    @Transactional
    public ReceptionRoom updateRoom(Long id, ReceptionRoomRequest request) {
        ReceptionRoom room = roomMapper.selectById(id);
        if (room == null) {
            throw new RuntimeException("接待室不存在");
        }
        room.setRoomName(request.getRoomName());
        room.setRoomCode(request.getRoomCode());
        room.setFloorId(request.getFloorId());
        room.setCapacity(request.getCapacity());
        room.setEquipmentCount(request.getEquipmentCount());
        room.setStatus(request.getStatus());
        roomMapper.updateById(room);
        try {
            redisCacheService.invalidateRoomCache(request.getFloorId());
        } catch (Exception e) {
        }
        return room;
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        ReceptionRoom room = roomMapper.selectById(id);
        if (room == null) {
            throw new RuntimeException("接待室不存在");
        }
        roomMapper.deleteById(id);
        try {
            redisCacheService.invalidateRoomCache(room.getFloorId());
        } catch (Exception e) {
        }
    }

    @Override
    public ReceptionRoom getRoomById(Long id) {
        return roomMapper.selectById(id);
    }

    @Override
    public List<ReceptionRoom> getRoomsByFloor(Long floorId) {
        try {
            Object cached = redisCacheService.getCachedRoomsByFloor(floorId);
            if (cached != null) {
                return (List<ReceptionRoom>) cached;
            }
        } catch (Exception e) {
        }
        LambdaQueryWrapper<ReceptionRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReceptionRoom::getFloorId, floorId);
        wrapper.eq(ReceptionRoom::getStatus, 1);
        List<ReceptionRoom> rooms = roomMapper.selectList(wrapper);
        try {
            redisCacheService.cacheRoomsByFloor(floorId, rooms);
        } catch (Exception e) {
        }
        return rooms;
    }

    @Override
    public List<ReceptionRoom> getAllRooms() {
        LambdaQueryWrapper<ReceptionRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ReceptionRoom::getFloorId);
        return roomMapper.selectList(wrapper);
    }
}

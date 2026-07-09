package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.request.ReceptionRoomRequest;
import com.example.devicemanagement.entity.ReceptionRoom;

import java.util.List;

public interface ReceptionRoomService {

    ReceptionRoom createRoom(ReceptionRoomRequest request);

    ReceptionRoom updateRoom(Long id, ReceptionRoomRequest request);

    void deleteRoom(Long id);

    ReceptionRoom getRoomById(Long id);

    List<ReceptionRoom> getRoomsByFloor(Long floorId);

    List<ReceptionRoom> getAllRooms();
}

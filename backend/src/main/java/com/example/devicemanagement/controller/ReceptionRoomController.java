package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.request.ReceptionRoomRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.service.ReceptionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
@CrossOrigin(origins = "*")
public class ReceptionRoomController {

    @Autowired
    private ReceptionRoomService roomService;

    @PostMapping
    public ApiResponse<ReceptionRoom> createRoom(@RequestBody ReceptionRoomRequest request) {
        ReceptionRoom room = roomService.createRoom(request);
        return ApiResponse.success(room);
    }

    @PutMapping("/{id}")
    public ApiResponse<ReceptionRoom> updateRoom(@PathVariable Long id, @RequestBody ReceptionRoomRequest request) {
        ReceptionRoom room = roomService.updateRoom(id, request);
        return ApiResponse.success(room);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReceptionRoom> getRoomById(@PathVariable Long id) {
        ReceptionRoom room = roomService.getRoomById(id);
        return ApiResponse.success(room);
    }

    @GetMapping("/floor/{floorId}")
    public ApiResponse<List<ReceptionRoom>> getRoomsByFloor(@PathVariable Long floorId) {
        List<ReceptionRoom> rooms = roomService.getRoomsByFloor(floorId);
        return ApiResponse.success(rooms);
    }

    @GetMapping
    public ApiResponse<List<ReceptionRoom>> getAllRooms() {
        List<ReceptionRoom> rooms = roomService.getAllRooms();
        return ApiResponse.success(rooms);
    }
}

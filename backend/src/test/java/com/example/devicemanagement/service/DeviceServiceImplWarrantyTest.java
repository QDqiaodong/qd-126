package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.devicemanagement.dto.response.WarrantyDeviceVO;
import com.example.devicemanagement.dto.response.WarrantyOverviewVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.service.impl.DeviceServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplWarrantyTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @BeforeAll
    static void initTableInfo() {
        // 单测环境没有 MyBatis 会话，需手动初始化实体表信息才能解析 Lambda 包装器
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
    }

    private Floor buildFloor(Long id, String name, int number) {
        Floor floor = new Floor();
        floor.setId(id);
        floor.setFloorName(name);
        floor.setFloorNumber(number);
        return floor;
    }

    private ReceptionRoom buildRoom(Long id, String name, Long floorId) {
        ReceptionRoom room = new ReceptionRoom();
        room.setId(id);
        room.setRoomName(name);
        room.setFloorId(floorId);
        return room;
    }

    private Device buildDevice(Long id, String code, Long floorId, Long roomId, LocalDate warrantyEndDate) {
        Device device = new Device();
        device.setId(id);
        device.setDeviceCode(code);
        device.setDeviceName("设备" + code);
        device.setDeviceType("电视");
        device.setCurrentFloorId(floorId);
        device.setCurrentRoomId(roomId);
        device.setWarrantyEndDate(warrantyEndDate);
        return device;
    }

    @Test
    void computesStatusDaysAndSeparatesUndatedDevices() {
        LocalDate today = LocalDate.now();
        when(floorService.getAllFloors()).thenReturn(List.of(buildFloor(1L, "3F", 3)));
        when(roomService.getAllRooms()).thenReturn(List.of(buildRoom(10L, "301接待室", 1L)));
        when(deviceMapper.selectList(any())).thenReturn(List.of(
                buildDevice(1L, "TV-001", 1L, 10L, today.plusDays(15)),
                buildDevice(2L, "TV-002", 1L, 10L, today.minusDays(3)),
                buildDevice(3L, "TV-003", 1L, 10L, today.plusDays(14)),
                buildDevice(4L, "TV-004", 1L, 10L, null)
        ));

        WarrantyOverviewVO overview = deviceService.getWarrantyOverview(null, null);

        assertEquals(15, overview.getExpiringSoonDays());
        assertEquals(1, overview.getGroups().size());
        assertEquals("3F", overview.getGroups().get(0).getFloorName());

        // 组内按剩余天数升序：已过期 -> 临期 -> 正常
        List<WarrantyDeviceVO> devices = overview.getGroups().get(0).getDevices();
        assertEquals(3, devices.size());

        WarrantyDeviceVO expired = devices.get(0);
        assertEquals(2L, expired.getId());
        assertEquals("EXPIRED", expired.getWarrantyStatus());
        assertEquals("已过期", expired.getWarrantyStatusText());
        assertEquals(-3L, expired.getDaysRemaining());

        WarrantyDeviceVO expiring = devices.get(1);
        assertEquals(3L, expiring.getId());
        assertEquals("EXPIRING", expiring.getWarrantyStatus());
        assertEquals(14L, expiring.getDaysRemaining());

        // 恰好15天不属于"不足15天"，为正常
        WarrantyDeviceVO normal = devices.get(2);
        assertEquals(1L, normal.getId());
        assertEquals("NORMAL", normal.getWarrantyStatus());
        assertEquals(15L, normal.getDaysRemaining());

        assertEquals("301接待室", expired.getCurrentRoomName());

        // 无截止日期的设备单独列出，不进入楼层分组
        assertEquals(1, overview.getNoWarrantyDevices().size());
        WarrantyDeviceVO undated = overview.getNoWarrantyDevices().get(0);
        assertEquals(4L, undated.getId());
        assertEquals("NONE", undated.getWarrantyStatus());
        assertEquals("未设置", undated.getWarrantyStatusText());
        assertNull(undated.getDaysRemaining());
    }

    @Test
    void appliesFloorAndRoomFiltersToQuery() {
        when(floorService.getAllFloors()).thenReturn(List.of(buildFloor(1L, "3F", 3)));
        when(roomService.getAllRooms()).thenReturn(List.of(buildRoom(10L, "301接待室", 1L)));
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        deviceService.getWarrantyOverview(1L, 10L);

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("current_floor_id"));
        assertTrue(sqlSegment.contains("current_room_id"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(1L));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(10L));
    }

    @Test
    void groupsByFloorInFloorOrderAndPutsUnassignedLast() {
        LocalDate today = LocalDate.now();
        when(floorService.getAllFloors()).thenReturn(List.of(
                buildFloor(1L, "1F", 1),
                buildFloor(2L, "2F", 2)
        ));
        when(roomService.getAllRooms()).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(
                buildDevice(1L, "TV-001", 2L, null, today.plusDays(30)),
                buildDevice(2L, "TV-002", 1L, null, today.plusDays(30)),
                buildDevice(3L, "TV-003", null, null, today.plusDays(30)),
                buildDevice(4L, "TV-004", 99L, null, today.plusDays(30))
        ));

        WarrantyOverviewVO overview = deviceService.getWarrantyOverview(null, null);

        assertEquals(3, overview.getGroups().size());
        assertEquals("1F", overview.getGroups().get(0).getFloorName());
        assertEquals(1, overview.getGroups().get(0).getDevices().size());
        assertEquals("2F", overview.getGroups().get(1).getFloorName());
        assertEquals(1, overview.getGroups().get(1).getDevices().size());

        // 未分配楼层及楼层已不存在的设备归入最后的未分配组，不会丢失
        assertEquals("未分配楼层", overview.getGroups().get(2).getFloorName());
        assertNull(overview.getGroups().get(2).getFloorId());
        assertEquals(2, overview.getGroups().get(2).getDevices().size());

        assertTrue(overview.getNoWarrantyDevices().isEmpty());
    }
}

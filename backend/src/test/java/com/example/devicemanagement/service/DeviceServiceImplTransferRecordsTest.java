package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.response.TransferRecordVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceTransfer;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceTransferMapper;
import com.example.devicemanagement.service.impl.DeviceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTransferRecordsTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceTransferMapper transferMapper;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DeviceSpecValidator deviceSpecValidator;

    @Mock
    private SpecTemplateService specTemplateService;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @BeforeAll
    static void initTableInfo() {
        // 单测环境没有 MyBatis 会话，需手动初始化实体表信息才能解析 Lambda 包装器
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, DeviceTransfer.class);
    }

    private DeviceTransfer buildTransfer(Long id, Long deviceId, String operator) {
        DeviceTransfer transfer = new DeviceTransfer();
        transfer.setId(id);
        transfer.setDeviceId(deviceId);
        transfer.setDeviceCode("DEV" + id);
        transfer.setOperator(operator);
        transfer.setTransferTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        return transfer;
    }

    @Test
    void filtersByDeviceNameAndOperator() {
        Device device = new Device();
        device.setId(7L);
        device.setDeviceName("高清投影仪");
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));
        when(deviceMapper.selectById(7L)).thenReturn(device);

        DeviceTransfer transfer = buildTransfer(1L, 7L, "张三");
        Page<DeviceTransfer> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(transfer));
        mapperPage.setTotal(1);
        when(transferMapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

        IPage<TransferRecordVO> result = deviceService.getAllTransferRecords(1, 10, "投影", "张");

        // 设备名称先解析为设备ID集合
        ArgumentCaptor<LambdaQueryWrapper> deviceWrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceMapper).selectList(deviceWrapperCaptor.capture());
        assertTrue(deviceWrapperCaptor.getValue().getSqlSegment().contains("device_name"));
        assertTrue(deviceWrapperCaptor.getValue().getParamNameValuePairs().containsValue("%投影%"));

        // 流转记录按设备ID与操作人过滤
        ArgumentCaptor<LambdaQueryWrapper> transferWrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(transferMapper).selectPage(any(Page.class), transferWrapperCaptor.capture());
        String sqlSegment = transferWrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("device_id"));
        assertTrue(sqlSegment.contains("IN"));
        assertTrue(sqlSegment.contains("operator"));
        assertTrue(transferWrapperCaptor.getValue().getParamNameValuePairs().containsValue("%张%"));

        // 总数来自 Mapper 分页结果而非前端估算
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("高清投影仪", result.getRecords().get(0).getDeviceName());
        assertEquals("张三", result.getRecords().get(0).getOperator());
    }

    @Test
    void returnsEmptyPageWhenDeviceNameMatchesNothing() {
        when(deviceMapper.selectList(any())).thenReturn(Collections.emptyList());

        IPage<TransferRecordVO> result = deviceService.getAllTransferRecords(1, 10, "不存在的设备", null);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(transferMapper, never()).selectPage(any(), any());
    }

    @Test
    void queriesAllWithRealTotalWhenFiltersBlank() {
        DeviceTransfer first = buildTransfer(1L, 7L, "张三");
        DeviceTransfer second = buildTransfer(2L, 8L, "李四");
        Page<DeviceTransfer> mapperPage = new Page<>(2, 10);
        mapperPage.setRecords(List.of(first, second));
        mapperPage.setTotal(25);
        when(transferMapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

        IPage<TransferRecordVO> result = deviceService.getAllTransferRecords(2, 10, "  ", null);

        // 空白筛选不触发设备查询
        verify(deviceMapper, never()).selectList(any());

        // 分页参数原样传递
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(transferMapper).selectPage(pageCaptor.capture(), any());
        assertEquals(2, pageCaptor.getValue().getCurrent());
        assertEquals(10, pageCaptor.getValue().getSize());

        assertEquals(25L, result.getTotal());
        assertEquals(2, result.getRecords().size());
    }
}

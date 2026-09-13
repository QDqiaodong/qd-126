package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.devicemanagement.dto.request.SpecFieldRequest;
import com.example.devicemanagement.dto.request.SpecTemplateRequest;
import com.example.devicemanagement.dto.response.SpecTemplatePreviewVO;
import com.example.devicemanagement.dto.response.SpecTemplateVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceSpecField;
import com.example.devicemanagement.entity.DeviceSpecTemplate;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceSpecFieldMapper;
import com.example.devicemanagement.mapper.DeviceSpecTemplateMapper;
import com.example.devicemanagement.service.impl.SpecTemplateServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecTemplateServiceImplPreviewTest {

    @Mock
    private DeviceSpecTemplateMapper templateMapper;

    @Mock
    private DeviceSpecFieldMapper fieldMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SpecTemplateServiceImpl specTemplateService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DeviceSpecTemplate.class);
        TableInfoHelper.initTableInfo(assistant, DeviceSpecField.class);
        TableInfoHelper.initTableInfo(assistant, Device.class);
    }

    private DeviceSpecTemplate buildTemplate() {
        DeviceSpecTemplate template = new DeviceSpecTemplate();
        template.setId(1L);
        template.setDeviceType("电视");
        template.setStatus(1);
        return template;
    }

    private DeviceSpecField oldField(Long id, String key, String label, String type) {
        DeviceSpecField field = new DeviceSpecField();
        field.setId(id);
        field.setTemplateId(1L);
        field.setFieldKey(key);
        field.setFieldLabel(label);
        field.setFieldType(type);
        field.setRequired(0);
        field.setSortOrder((int) id.longValue());
        return field;
    }

    private SpecFieldRequest newFieldRequest(String key, String label, String type) {
        SpecFieldRequest request = new SpecFieldRequest();
        request.setFieldKey(key);
        request.setFieldLabel(label);
        request.setFieldType(type);
        request.setRequired(false);
        request.setSortOrder(0);
        return request;
    }

    private Device buildDevice(Long id, String specJson) {
        Device device = new Device();
        device.setId(id);
        device.setDeviceName("设备" + id);
        device.setDeviceType("电视");
        device.setSpecJson(specJson);
        return device;
    }

    @Test
    void previewComputesAddRemoveTypeChangeAndAffectedDeviceCount() {
        when(templateMapper.selectById(1L)).thenReturn(buildTemplate());
        when(fieldMapper.selectList(any())).thenReturn(List.of(
                oldField(1L, "screenSize", "屏幕尺寸", "number"),
                oldField(2L, "resolution", "分辨率", "select"),
                oldField(3L, "panelType", "面板类型", "select")
        ));
        when(deviceMapper.selectList(any())).thenReturn(List.of(
                buildDevice(1L, "{\"screenSize\":55,\"resolution\":\"4K\"}"),
                buildDevice(2L, "{\"screenSize\":65}"),
                buildDevice(3L, "{\"panelType\":\"OLED\"}"),
                buildDevice(4L, "{}")
        ));

        SpecTemplateRequest request = new SpecTemplateRequest();
        request.setDeviceType("电视");
        request.setStatus(1);
        request.setFields(List.of(
                newFieldRequest("screenSize", "屏幕尺寸", "number"),
                newFieldRequest("resolution", "分辨率", "text"),
                newFieldRequest("brightness", "亮度(流明)", "number")
        ));

        SpecTemplatePreviewVO preview = specTemplateService.previewChanges(1L, request);

        assertEquals(1L, preview.getTemplateId());
        assertEquals("电视", preview.getDeviceType());
        assertEquals(4, preview.getTotalDeviceCount());
        // 仅类型变化(resolution)与删除(panelType)的已存值计入受影响；screenSize 未变、空规格不计
        assertEquals(2, preview.getAffectedDeviceCount());
        // 正在引用该模板的设备名称逐台返回，供保存前弹窗展示
        assertEquals(List.of("设备1", "设备2", "设备3", "设备4"), preview.getReferencingDeviceNames());
        assertTrue(preview.getChanged());

        assertEquals(List.of("brightness"),
                preview.getAddedFields().stream().map(f -> f.getFieldKey()).toList());
        assertEquals(List.of("panelType"),
                preview.getRemovedFields().stream().map(f -> f.getFieldKey()).toList());
        assertEquals(1, preview.getTypeChangedFields().size());
        assertEquals("select", preview.getTypeChangedFields().get(0).getOldFieldType());
        assertEquals("text", preview.getTypeChangedFields().get(0).getNewFieldType());

        // 预览只做差异计算，不落库
        verify(fieldMapper, never()).delete(any());
        verify(fieldMapper, never()).insert(any());
        verify(templateMapper, never()).updateById(any());
    }

    @Test
    void previewWithoutReferencingDevicesReturnsEmptyNames() {
        when(templateMapper.selectById(1L)).thenReturn(buildTemplate());
        when(fieldMapper.selectList(any())).thenReturn(List.of(
                oldField(1L, "screenSize", "屏幕尺寸", "number")));
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        SpecTemplateRequest request = new SpecTemplateRequest();
        request.setDeviceType("电视");
        request.setStatus(1);
        request.setFields(List.of(newFieldRequest("screenSize", "屏幕尺寸", "text")));

        SpecTemplatePreviewVO preview = specTemplateService.previewChanges(1L, request);

        assertEquals(0, preview.getTotalDeviceCount());
        assertEquals(0, preview.getAffectedDeviceCount());
        assertTrue(preview.getReferencingDeviceNames().isEmpty());
    }

    @Test
    void previewRejectsInvalidFieldDefinition() {
        when(templateMapper.selectById(1L)).thenReturn(buildTemplate());

        SpecTemplateRequest request = new SpecTemplateRequest();
        request.setDeviceType("电视");
        request.setStatus(1);
        SpecFieldRequest badSelect = newFieldRequest("resolution", "分辨率", "select");
        badSelect.setOptions(List.of());
        request.setFields(List.of(badSelect));

        assertThrows(IllegalArgumentException.class,
                () -> specTemplateService.previewChanges(1L, request));

        verify(fieldMapper, never()).delete(any());
        verify(fieldMapper, never()).insert(any());
    }

    @Test
    void updateWithInvalidFieldsLeavesOldDefinitionUntouched() {
        when(templateMapper.selectById(1L)).thenReturn(buildTemplate());

        SpecTemplateRequest request = new SpecTemplateRequest();
        request.setDeviceType("电视");
        request.setStatus(1);
        // 非法类型：在任何写库动作前就应失败
        request.setFields(List.of(newFieldRequest("screenSize", "屏幕尺寸", "unsupported")));

        assertThrows(IllegalArgumentException.class,
                () -> specTemplateService.updateTemplate(1L, request));

        // 不产生半成品：模板行不更新，字段不删除/不插入
        verify(templateMapper, never()).updateById(any());
        verify(fieldMapper, never()).delete(any());
        verify(fieldMapper, never()).insert(any());
    }

    @Test
    void disableTemplateDoesNotTouchFieldsOrDeviceSpecs() {
        when(templateMapper.selectById(1L)).thenReturn(buildTemplate());
        when(fieldMapper.selectList(any())).thenReturn(List.of(
                oldField(1L, "screenSize", "屏幕尺寸", "number")));

        SpecTemplateVO vo = specTemplateService.updateStatus(1L, 0);

        assertEquals(0, vo.getStatus());
        // 停用只改模板状态行：字段定义保留，设备表完全不涉及
        ArgumentCaptor<DeviceSpecTemplate> templateCaptor = ArgumentCaptor.forClass(DeviceSpecTemplate.class);
        verify(templateMapper).updateById(templateCaptor.capture());
        assertEquals(Integer.valueOf(0), templateCaptor.getValue().getStatus());
        verify(fieldMapper, never()).delete(any());
        verify(deviceMapper, never()).updateById(any());
    }
}

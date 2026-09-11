package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.response.SpecFieldVO;
import com.example.devicemanagement.service.impl.DeviceSpecValidatorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSpecValidatorImplTest {

    @Mock
    private SpecTemplateService specTemplateService;

    @InjectMocks
    private DeviceSpecValidatorImpl validator;

    private SpecFieldVO field(String key, String label, String type, boolean required) {
        SpecFieldVO vo = new SpecFieldVO();
        vo.setFieldKey(key);
        vo.setFieldLabel(label);
        vo.setFieldType(type);
        vo.setRequired(required);
        vo.setOptions("select".equals(type) ? List.of("1080P", "4K") : List.of());
        vo.setSortOrder(1);
        return vo;
    }

    @Test
    void removedFieldHistoricalValuesAreKeptWhenTemplateChanges() {
        // 新模板只剩 screenSize，resolution 字段已被删除
        when(specTemplateService.getEnabledFields("电视"))
                .thenReturn(List.of(field("screenSize", "屏幕尺寸", "number", true)));

        Map<String, Object> submitted = new LinkedHashMap<>();
        submitted.put("screenSize", 65);
        submitted.put("resolution", "4K"); // 历史设备保存过、模板已删除的字段

        Map<String, Object> normalized = validator.validate("电视", submitted);

        assertEquals(65, ((Number) normalized.get("screenSize")).intValue());
        // 历史规格不被模板修改覆盖/清除
        assertEquals("4K", normalized.get("resolution"));
    }

    @Test
    void disabledTemplateSkipsEnforcementAndKeepsRawSpec() {
        when(specTemplateService.getEnabledFields("音响")).thenReturn(List.of());

        Map<String, Object> submitted = new LinkedHashMap<>();
        submitted.put("power", 100);
        submitted.put("customKey", "自定义值");

        Map<String, Object> result = validator.validate("音响", submitted);

        // 模板停用期间不做强制校验，规格原样返回（重新启用后新设备才按最新字段校验）
        assertEquals(submitted, result);
    }

    @Test
    void requiredFieldIsEnforcedForNewDevicesOnLatestTemplate() {
        when(specTemplateService.getEnabledFields("麦克风"))
                .thenReturn(List.of(field("pickupPattern", "指向性", "select", true)));

        Map<String, Object> emptySpec = new LinkedHashMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("麦克风", emptySpec));
        assertTrue(ex.getMessage().contains("指向性"));
    }
}

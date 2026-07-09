package com.example.devicemanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {

    private static final String DEVICE_SPEC_TEMPLATE_KEY = "device:spec:template:";
    private static final String FLOOR_CACHE_KEY = "device:floor:list";
    private static final String ROOM_CACHE_KEY_PREFIX = "device:room:floor:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveDeviceSpecTemplate(String deviceType, Map<String, Object> specTemplate) {
        String key = DEVICE_SPEC_TEMPLATE_KEY + deviceType;
        redisTemplate.opsForHash().putAll(key, specTemplate);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    public Map<Object, Object> getDeviceSpecTemplate(String deviceType) {
        String key = DEVICE_SPEC_TEMPLATE_KEY + deviceType;
        return redisTemplate.opsForHash().entries(key);
    }

    public void deleteDeviceSpecTemplate(String deviceType) {
        String key = DEVICE_SPEC_TEMPLATE_KEY + deviceType;
        redisTemplate.delete(key);
    }

    public void cacheFloorList(Object floorList) {
        redisTemplate.opsForValue().set(FLOOR_CACHE_KEY, floorList, 10, TimeUnit.MINUTES);
    }

    public Object getCachedFloorList() {
        return redisTemplate.opsForValue().get(FLOOR_CACHE_KEY);
    }

    public void cacheRoomsByFloor(Long floorId, Object roomList) {
        String key = ROOM_CACHE_KEY_PREFIX + floorId;
        redisTemplate.opsForValue().set(key, roomList, 10, TimeUnit.MINUTES);
    }

    public Object getCachedRoomsByFloor(Long floorId) {
        String key = ROOM_CACHE_KEY_PREFIX + floorId;
        return redisTemplate.opsForValue().get(key);
    }

    public void invalidateFloorCache() {
        redisTemplate.delete(FLOOR_CACHE_KEY);
    }

    public void invalidateRoomCache(Long floorId) {
        String key = ROOM_CACHE_KEY_PREFIX + floorId;
        redisTemplate.delete(key);
    }
}

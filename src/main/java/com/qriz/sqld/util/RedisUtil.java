package com.qriz.sqld.util;

import org.springframework.stereotype.Service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisUtil {
    
    private final StringRedisTemplate redisTemplate;//Redis에 접근하기 위한 Spring의 Redis 템플릿 클래스
    private final Logger log = LoggerFactory.getLogger(RedisUtil.class);

    public String getData(String key){//지정된 키(key)에 해당하는 데이터를 Redis에서 가져오는 메서드
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(key);
    }
    public void setData(String key, String value){//지정된 키(key)에 값을 저장하는 메서드
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key,value);
    }
    public void setDataExpire(String key, String value, long duration){// 지정된 키(key)에 값을 저장하고, 지정된 시간(duration) 후에 데이터가 만료되도록 설정하는 메서드
        log.debug("디버그 : Redis에 데이터 설정 - 키: " + key + ", 값: " + value + ", 만료 시간(초 단위): " + duration);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (duration > 0) { // 만료 시간이 유효한지 확인
            Duration expireDuration = Duration.ofSeconds(duration);
            valueOperations.set(key, value, expireDuration);
        } else {
            log.error("디버그 : 유효하지 않은 만료 시간 - duration: " + duration);
            throw new IllegalArgumentException("유효하지 않은 만료 시간: " + duration);
        }
        Duration expireDuration = Duration.ofSeconds(duration);
        valueOperations.set(key, value, expireDuration);
    }
    public void deleteData(String key){//지정된 키(key)에 해당하는 데이터를 Redis에서 삭제하는 메서드
        log.debug("디버그 : Redis에서 데이터 삭제 - 키: " + key);
        redisTemplate.delete(key);
    }
}

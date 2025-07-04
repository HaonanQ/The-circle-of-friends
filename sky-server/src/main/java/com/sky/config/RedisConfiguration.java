package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {
    @Bean
    public RedisTemplate Redistempl(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate redisTemplate = new RedisTemplate();
        // 设置redis连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置key的序列化方式
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}

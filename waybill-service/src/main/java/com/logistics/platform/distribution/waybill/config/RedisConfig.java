package com.logistics.platform.distribution.waybill.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.platform.distribution.waybill.entity.Waybill;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Waybill> redisTemplate(RedisConnectionFactory Factory) {
        RedisTemplate<String,Waybill> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(Factory);
        ObjectMapper om=new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        //序列化配置
        Jackson2JsonRedisSerializer<Waybill> jacksonSerializer =
                new Jackson2JsonRedisSerializer<>(om, Waybill.class);


        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jacksonSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jacksonSerializer);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

}

package com.logistics.platform.distribution.waybill.service.impl;

import com.fasterxml.classmate.members.ResolvedMember;
import com.logistics.platform.distribution.waybill.entity.Waybill;
import com.logistics.platform.distribution.waybill.entity.WaybillStatus;
import com.logistics.platform.distribution.waybill.repository.WaybillRepository;
import com.logistics.platform.distribution.waybill.service.WaybillService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class WaybillServiceImpl implements WaybillService {

    private final WaybillRepository waybillRepository;
    private final RedisTemplate<String, Waybill> redisTemplate;

    private static final String WAYBILL_KEY="waybill:NO";
    private static final long WAYBILL_EXPIRE_TIME=30;


    @Override
    public Waybill createWaybill(Waybill waybill) {
        waybill.setStatus(WaybillStatus.CREATED);
        waybill.setCreateTime(LocalDateTime.now());
        waybill.setUpdateTime(LocalDateTime.now());
        //创建单号
        waybill.setWaybillNo("WB"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+ (int) (Math.random() * 1000));

        Waybill saveWaybill=waybillRepository.save(waybill);

        //缓存
        redisTemplate.opsForValue().set(
                WAYBILL_KEY+saveWaybill.getWaybillNo(),
                saveWaybill,
                WAYBILL_EXPIRE_TIME,
                TimeUnit.MINUTES
        );
        return saveWaybill;

    }

    @Override
    public Waybill updateWaybillStatus(String waybillNo, String status) {
        //先查缓存,缓存没有再查数据库
        Waybill waybill=getByWaybillNo(waybillNo)
                .orElseThrow(()->new IllegalArgumentException("运单不存在：" + waybillNo));

        //更新运单状态
        waybill.setStatus(WaybillStatus.fromString(status));
        waybill.setUpdateTime(LocalDateTime.now());
        if (status.equals(WaybillStatus.DELIVERING)){
            waybill.setActualArrivalTime(LocalDateTime.now());
        }
        Waybill updateWaybill=waybillRepository.save(waybill);

        //更新缓存
        redisTemplate.opsForValue().set(
                WAYBILL_KEY+waybill.getWaybillNo(),
                updateWaybill,
                WAYBILL_EXPIRE_TIME,
                TimeUnit.MINUTES
        );
        return updateWaybill;

    }

    @Override
    public Optional<Waybill> getByWaybillNo(String waybillNo) {
        Waybill cachedWaybill=redisTemplate.opsForValue().get(WAYBILL_KEY+waybillNo);
        if(cachedWaybill!=null){
            return Optional.of(cachedWaybill);
        }
        //查数据库
        Optional<Waybill> dbwaybill=waybillRepository.findByWaybillNo(waybillNo);
        dbwaybill.ifPresent(waybill -> redisTemplate.opsForValue().set(
                WAYBILL_KEY+waybill.getWaybillNo(),
                waybill,
                WAYBILL_EXPIRE_TIME,
                TimeUnit.MINUTES
        ));
        return dbwaybill;

    }

    @Override
    public List<Waybill> getAllWaybills() {

        return waybillRepository.findAll();
    }
}

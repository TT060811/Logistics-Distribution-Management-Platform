package com.logistics.platform.distribution.waybill.service.impl;

import com.logistics.platform.distribution.waybill.entity.Waybill;
import com.logistics.platform.distribution.waybill.entity.WaybillStatus;
import com.logistics.platform.distribution.waybill.repository.WaybillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaybillServiceImplTest {

    // Mock依赖组件
    @Mock
    private WaybillRepository waybillRepository;

    @Mock
    private RedisTemplate<String, Waybill> redisTemplate;

    @Mock
    private ValueOperations<String, Waybill> valueOperations;

    // 注入被测试的Service
    @InjectMocks
    private WaybillServiceImpl waybillService;

    // 常量（与ServiceImpl保持一致）
    private static final String WAYBILL_KEY = "waybill:NO";

    /**
     * 测试：创建运单（核心逻辑：生成运单号、设置默认状态、缓存写入）
     */
    @Test
    public void testCreateWaybill() {
        // 1. 构造入参
        Waybill waybillParam = new Waybill();
        waybillParam.setCustomerId(1001L);
        waybillParam.setSenderName("张三");
        waybillParam.setSenderPhone("13800138000");
        waybillParam.setReceiverName("李四");
        waybillParam.setReceiverPhone("13900139000");
        waybillParam.setGoodsType("电子产品");
        waybillParam.setWeight(new BigDecimal("2.5"));
        waybillParam.setAmount(new BigDecimal("50.00"));

        // 2. Mock Redis的ValueOperations（避免NPE）
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 3. Mock Repository.save返回结果（模拟数据库保存）
        Waybill savedWaybill = new Waybill();
        savedWaybill.setId(1L);
        savedWaybill.setWaybillNo("WB20251204153000123"); // 模拟生成的运单号
        savedWaybill.setCustomerId(waybillParam.getCustomerId());
        savedWaybill.setStatus(WaybillStatus.CREATED);
        savedWaybill.setCreateTime(LocalDateTime.now());
        savedWaybill.setUpdateTime(LocalDateTime.now());
        when(waybillRepository.save(any(Waybill.class))).thenReturn(savedWaybill);

        // 4. 执行创建运单方法
        Waybill result = waybillService.createWaybill(waybillParam);

        // 5. 验证核心逻辑
        assertNotNull(result);
        assertNotNull(result.getWaybillNo()); // 运单号已生成
        assertEquals(WaybillStatus.CREATED, result.getStatus()); // 默认状态为CREATED
        assertNotNull(result.getCreateTime()); // 创建时间已设置
        assertNotNull(result.getUpdateTime()); // 更新时间已设置
        assertEquals("WB", result.getWaybillNo().substring(0, 2)); // 运单号前缀正确

        // 6. 验证数据库保存和缓存写入
        verify(waybillRepository, times(1)).save(any(Waybill.class));
        verify(redisTemplate.opsForValue(), times(1)).set(
                eq(WAYBILL_KEY + result.getWaybillNo()),
                eq(result),
                eq(30L),
                eq(java.util.concurrent.TimeUnit.MINUTES)
        );
    }

    /**
     * 测试：更新运单状态（缓存命中场景）
     */
    @Test
    public void testUpdateWaybillStatus_CacheHit() {
        // 1. 构造测试数据
        String waybillNo = "WB20251204153000123";
        String newStatus = "DELIVERING";

        // 2. Mock缓存命中
        Waybill cachedWaybill = new Waybill();
        cachedWaybill.setWaybillNo(waybillNo);
        cachedWaybill.setStatus(WaybillStatus.CREATED);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WAYBILL_KEY + waybillNo)).thenReturn(cachedWaybill);

        // 3. Mock Repository.save返回更新后的运单
        Waybill updatedWaybill = new Waybill();
        updatedWaybill.setWaybillNo(waybillNo);
        updatedWaybill.setStatus(WaybillStatus.DELIVERING);
        updatedWaybill.setUpdateTime(LocalDateTime.now());
        updatedWaybill.setActualArrivalTime(LocalDateTime.now()); // DELIVERING状态会设置实际送达时间
        when(waybillRepository.save(any(Waybill.class))).thenReturn(updatedWaybill);

        // 4. 执行更新状态方法
        Waybill result = waybillService.updateWaybillStatus(waybillNo, newStatus);

        // 5. 验证结果
        assertEquals(WaybillStatus.DELIVERING, result.getStatus()); // 状态更新成功
        assertNotNull(result.getUpdateTime()); // 更新时间已刷新
        assertNotNull(result.getActualArrivalTime()); // DELIVERING状态设置了实际送达时间

        // 6. 验证缓存和数据库操作
        verify(valueOperations, times(1)).get(WAYBILL_KEY + waybillNo); // 查缓存
        verify(waybillRepository, times(1)).save(any(Waybill.class)); // 保存到数据库
        verify(valueOperations, times(1)).set( // 更新缓存
                eq(WAYBILL_KEY + waybillNo),
                eq(updatedWaybill),
                eq(30L),
                eq(java.util.concurrent.TimeUnit.MINUTES)
        );
    }

    /**
     * 测试：更新运单状态（缓存未命中，查数据库场景）
     */
    @Test
    public void testUpdateWaybillStatus_CacheMiss() {
        // 1. 构造测试数据
        String waybillNo = "WB20251204153000123";
        String newStatus = "DELIVERED";

        // 2. Mock缓存未命中
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WAYBILL_KEY + waybillNo)).thenReturn(null);

        // 3. Mock数据库查询到运单
        Waybill dbWaybill = new Waybill();
        dbWaybill.setWaybillNo(waybillNo);
        dbWaybill.setStatus(WaybillStatus.PICKED);
        when(waybillRepository.findByWaybillNo(waybillNo)).thenReturn(Optional.of(dbWaybill));

        // 4. Mock数据库保存更新后的运单
        Waybill updatedWaybill = new Waybill();
        updatedWaybill.setWaybillNo(waybillNo);
        updatedWaybill.setStatus(WaybillStatus.DELIVERED);
        updatedWaybill.setUpdateTime(LocalDateTime.now());
        when(waybillRepository.save(any(Waybill.class))).thenReturn(updatedWaybill);

        // 5. 执行更新状态方法
        Waybill result = waybillService.updateWaybillStatus(waybillNo, newStatus);

        // 6. 验证结果
        assertEquals(WaybillStatus.DELIVERED, result.getStatus());
        assertNull(result.getActualArrivalTime()); // DELIVERED状态不会设置实际送达时间（仅DELIVERING设置）

        // 7. 验证操作顺序：查缓存→查数据库→更新数据库→更新缓存
        verify(valueOperations, times(1)).get(WAYBILL_KEY + waybillNo); // 查缓存
        verify(waybillRepository, times(1)).findByWaybillNo(waybillNo); // 查数据库
        verify(waybillRepository, times(1)).save(any(Waybill.class)); // 保存数据库
        verify(valueOperations, times(1)).set( // 更新缓存
                eq(WAYBILL_KEY + waybillNo),
                eq(updatedWaybill),
                eq(30L),
                eq(java.util.concurrent.TimeUnit.MINUTES)
        );
    }

    /**
     * 测试：更新不存在的运单（抛异常）
     */
    @Test
    public void testUpdateWaybillStatus_WaybillNotFound() {
        // 1. 构造测试数据
        String nonExistentWaybillNo = "WB999999999999999";
        String newStatus = "DELIVERING";

        // 2. Mock缓存和数据库都未找到运单
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WAYBILL_KEY + nonExistentWaybillNo)).thenReturn(null);
        when(waybillRepository.findByWaybillNo(nonExistentWaybillNo)).thenReturn(Optional.empty());

        // 3. 执行方法，验证抛异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            waybillService.updateWaybillStatus(nonExistentWaybillNo, newStatus);
        });
        assertEquals("运单不存在：" + nonExistentWaybillNo, exception.getMessage());

        // 4. 验证无数据库保存操作
        verify(waybillRepository, never()).save(any(Waybill.class));
    }

    /**
     * 测试：按运单号查询（缓存命中）
     */
    @Test
    public void testGetByWaybillNo_CacheHit() {
        // 1. 构造测试数据
        String waybillNo = "WB20251204153000123";

        // 2. Mock缓存命中
        Waybill cachedWaybill = new Waybill();
        cachedWaybill.setWaybillNo(waybillNo);
        cachedWaybill.setSenderName("张三");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WAYBILL_KEY + waybillNo)).thenReturn(cachedWaybill);

        // 3. 执行查询方法
        Optional<Waybill> result = waybillService.getByWaybillNo(waybillNo);

        // 4. 验证结果
        assertTrue(result.isPresent());
        assertEquals(waybillNo, result.get().getWaybillNo());
        assertEquals("张三", result.get().getSenderName());

        // 5. 验证未查询数据库
        verify(waybillRepository, never()).findByWaybillNo(waybillNo);
    }

    /**
     * 测试：按运单号查询（缓存未命中，查数据库并写入缓存）
     */
    @Test
    public void testGetByWaybillNo_CacheMiss() {
        // 1. 构造测试数据
        String waybillNo = "WB20251204153000123";

        // 2. Mock缓存未命中，数据库命中
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WAYBILL_KEY + waybillNo)).thenReturn(null);

        Waybill dbWaybill = new Waybill();
        dbWaybill.setWaybillNo(waybillNo);
        dbWaybill.setReceiverName("李四");
        when(waybillRepository.findByWaybillNo(waybillNo)).thenReturn(Optional.of(dbWaybill));

        // 3. 执行查询方法
        Optional<Waybill> result = waybillService.getByWaybillNo(waybillNo);

        // 4. 验证结果
        assertTrue(result.isPresent());
        assertEquals(waybillNo, result.get().getWaybillNo());
        assertEquals("李四", result.get().getReceiverName());

        // 5. 验证数据库查询和缓存写入
        verify(waybillRepository, times(1)).findByWaybillNo(waybillNo);
        verify(valueOperations, times(1)).set(
                eq(WAYBILL_KEY + waybillNo),
                eq(dbWaybill),
                eq(30L),
                eq(java.util.concurrent.TimeUnit.MINUTES)
        );
    }

    /**
     * 测试：查询所有运单
     */
    @Test
    public void testGetAllWaybills() {
        // 1. Mock数据库返回运单列表
        Waybill waybill1 = new Waybill();
        waybill1.setWaybillNo("WB20251204153000123");
        Waybill waybill2 = new Waybill();
        waybill2.setWaybillNo("WB20251204153000456");
        List<Waybill> mockWaybillList = List.of(waybill1, waybill2);
        when(waybillRepository.findAll()).thenReturn(mockWaybillList);

        // 2. 执行查询方法
        List<Waybill> result = waybillService.getAllWaybills();

        // 3. 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("WB20251204153000123", result.get(0).getWaybillNo());
        assertEquals("WB20251204153000456", result.get(1).getWaybillNo());

        // 4. 验证数据库查询
        verify(waybillRepository, times(1)).findAll();
    }
}
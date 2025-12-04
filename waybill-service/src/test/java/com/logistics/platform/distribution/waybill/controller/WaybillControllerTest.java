package com.logistics.platform.distribution.waybill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.platform.distribution.waybill.entity.Waybill;
import com.logistics.platform.distribution.waybill.entity.WaybillStatus;
import com.logistics.platform.distribution.waybill.service.WaybillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WaybillController.class)
public class WaybillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WaybillService waybillService;

    // 初始化ObjectMapper，支持LocalDateTime序列化
    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 测试：POST /waybill（创建运单接口）
     * 适配你的Controller路径和返回值
     */
    @Test
    public void testCreateWaybill() throws Exception {
        // 1. 构造请求体（匹配你的实体类字段）
        Waybill waybillParam = new Waybill();
        waybillParam.setCustomerId(1001L);
        waybillParam.setSenderName("张三");
        waybillParam.setSenderPhone("13800138000");
        waybillParam.setSenderAddress("北京市海淀区XX路XX号");
        waybillParam.setReceiverName("李四");
        waybillParam.setReceiverPhone("13900139000");
        waybillParam.setReceiverAddress("上海市浦东新区XX路XX号");
        waybillParam.setGoodsType("电子产品");
        waybillParam.setWeight(new BigDecimal("2.5"));
        waybillParam.setVolume(new BigDecimal("0.01"));
        waybillParam.setAmount(new BigDecimal("50.00"));

        // 2. Mock Service返回结果
        Waybill mockResult = new Waybill();
        mockResult.setId(1L);
        mockResult.setWaybillNo("WB20251204153000123"); // 模拟生成的运单号
        mockResult.setCustomerId(1001L);
        mockResult.setStatus(WaybillStatus.CREATED);
        mockResult.setCreateTime(LocalDateTime.now());
        mockResult.setUpdateTime(LocalDateTime.now());
        when(waybillService.createWaybill(any(Waybill.class))).thenReturn(mockResult);

        // 3. 模拟POST请求（路径为/waybill，适配你的Controller）
        mockMvc.perform(post("/waybill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waybillParam)))
                // 4. 验证响应（适配你的返回值：ResponseEntity.ok(Waybill)）
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.waybillNo").value("WB20251204153000123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerId").value(1001L))
                .andExpect(jsonPath("$.amount").value(50.00));

        // 5. 验证Service调用
        verify(waybillService, times(1)).createWaybill(any(Waybill.class));
    }

    /**
     * 测试：GET /waybill/{waybillNo}（按运单号查询接口）
     * 适配你的Controller路径和返回值（存在则200，不存在则404）
     */
    @Test
    public void testGetWaybillByNo_Exists() throws Exception {
        // 1. 构造测试数据
        String waybillNo = "WB20251204153000123";

        // 2. Mock Service返回存在的运单
        Waybill mockWaybill = new Waybill();
        mockWaybill.setWaybillNo(waybillNo);
        mockWaybill.setSenderName("张三");
        mockWaybill.setReceiverName("李四");
        mockWaybill.setStatus(WaybillStatus.CREATED);
        when(waybillService.getByWaybillNo(eq(waybillNo))).thenReturn(Optional.of(mockWaybill));

        // 3. 模拟GET请求（路径/waybill/{waybillNo}）
        mockMvc.perform(get("/waybill/{waybillNo}", waybillNo)
                        .contentType(MediaType.APPLICATION_JSON))
                // 4. 验证响应（存在则返回200 + 运单数据）
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waybillNo").value(waybillNo))
                .andExpect(jsonPath("$.senderName").value("张三"))
                .andExpect(jsonPath("$.receiverName").value("李四"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        // 5. 验证Service调用
        verify(waybillService, times(1)).getByWaybillNo(eq(waybillNo));
    }

    /**
     * 测试：GET /waybill/{waybillNo}（查询不存在的运单，返回404）
     * 适配你的Controller的orElse(ResponseEntity.notFound().build())逻辑
     */
    @Test
    public void testGetWaybillByNo_NotFound() throws Exception {
        // 1. 构造测试数据
        String nonExistentWaybillNo = "WB999999999999999";

        // 2. Mock Service返回空
        when(waybillService.getByWaybillNo(eq(nonExistentWaybillNo))).thenReturn(Optional.empty());

        // 3. 模拟GET请求
        mockMvc.perform(get("/waybill/{waybillNo}", nonExistentWaybillNo)
                        .contentType(MediaType.APPLICATION_JSON))
                // 4. 验证响应（404 Not Found）
                .andExpect(status().isNotFound());

        // 5. 验证Service调用
        verify(waybillService, times(1)).getByWaybillNo(eq(nonExistentWaybillNo));
    }

    /**
     * 测试：GET /waybill（查询所有运单接口）
     * 适配你的Controller路径和返回值（ResponseEntity<List<Waybill>>）
     */
    @Test
    public void testGetAllWaybills() throws Exception {
        // 1. Mock Service返回运单列表
        Waybill waybill1 = new Waybill();
        waybill1.setWaybillNo("WB20251204153000123");
        waybill1.setStatus(WaybillStatus.CREATED);
        waybill1.setSenderName("张三");

        Waybill waybill2 = new Waybill();
        waybill2.setWaybillNo("WB20251204153000456");
        waybill2.setStatus(WaybillStatus.DELIVERING);
        waybill2.setSenderName("王五");

        List<Waybill> mockWaybillList = List.of(waybill1, waybill2);
        when(waybillService.getAllWaybills()).thenReturn(mockWaybillList);

        // 2. 模拟GET请求（路径/waybill）
        mockMvc.perform(get("/waybill")
                        .contentType(MediaType.APPLICATION_JSON))
                // 3. 验证响应
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.size()").value(2)) // 列表长度为2
                .andExpect(jsonPath("$[0].waybillNo").value("WB20251204153000123"))
                .andExpect(jsonPath("$[0].senderName").value("张三"))
                .andExpect(jsonPath("$[1].waybillNo").value("WB20251204153000456"))
                .andExpect(jsonPath("$[1].status").value("DELIVERING"));

        // 4. 验证Service调用
        verify(waybillService, times(1)).getAllWaybills();
    }
}
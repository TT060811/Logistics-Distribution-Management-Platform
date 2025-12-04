package com.logistics.platform.distribution.waybill.controller;


import com.logistics.platform.distribution.waybill.entity.Waybill;
import com.logistics.platform.distribution.waybill.service.WaybillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/waybill")
@RequiredArgsConstructor
public class WaybillController {
    private final WaybillService waybillService;
    //创建运单
    @PostMapping
    public ResponseEntity<Waybill> createWaybill(@RequestBody Waybill waybill) {
        return ResponseEntity.ok(waybillService.createWaybill(waybill));
    }

    //查询运单
    @GetMapping("/{waybillNo}")
    public ResponseEntity<Waybill> getWaybillByNo(@PathVariable String waybillNo){
        return waybillService.getByWaybillNo(waybillNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    //查询所有订单
    @GetMapping
    public ResponseEntity<List<Waybill>> getAllWaybills(){
        return ResponseEntity.ok(waybillService.getAllWaybills());
    }



}

package com.logistics.platform.distribution.waybill.controller;


import com.logistics.platform.distribution.waybill.entity.Waybill;
import com.logistics.platform.distribution.waybill.entity.WaybillStatus;
import com.logistics.platform.distribution.waybill.service.WaybillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/waybill/status")
@RequiredArgsConstructor
public class WaybillStatusController {
    private final WaybillService waybillService;

    @PutMapping("/{waybillNo}")
    public ResponseEntity<Waybill> updateWaybillStatus(
            @PathVariable String waybillNo,
            @RequestParam String status){
        return ResponseEntity.ok(waybillService.updateWaybillStatus(waybillNo, status));
    }
}

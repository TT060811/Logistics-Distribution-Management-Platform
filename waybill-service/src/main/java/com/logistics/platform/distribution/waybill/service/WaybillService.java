package com.logistics.platform.distribution.waybill.service;

import com.logistics.platform.distribution.waybill.entity.Waybill;
import com.logistics.platform.distribution.waybill.entity.WaybillStatus;

import java.util.List;
import java.util.Optional;

public interface WaybillService {
    //创建运单
    Waybill createWaybill(Waybill waybill);

    //更新运单
    Waybill updateWaybillStatus(String waybillNo, String status);

    //运单号查询
    Optional<Waybill> getByWaybillNo(String waybillNo);

    //查询所有运单
    List<Waybill> getAllWaybills();


}

package com.logistics.platform.distribution.waybill.repository;


import com.logistics.platform.distribution.waybill.entity.Waybill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaybillRepository extends JpaRepository<Waybill,Long> {
    Optional<Waybill> findByWaybillNo(String waybillNo);
}

package com.logistics.platform.distribution.waybill.entity;

import java.util.Arrays;
import java.util.Set;

public enum WaybillStatus {

    CREATED("已创建"),

    PICKED("已揽收"),

    DELIVERING("配送中"),

    DELIVERED("已送达"),

    CANCELLED("已取消");

    private String desc;

    private static final java.util.Map<WaybillStatus, Set<WaybillStatus>> VALID_TRANSITIONS;
    static {
        // 初始化状态流转规则
        VALID_TRANSITIONS = new java.util.HashMap<>();
        // CREATED状态可流转到：PICKED/CANCELLED
        VALID_TRANSITIONS.put(CREATED, Set.of(PICKED, CANCELLED));
        // PICKED状态可流转到：DELIVERING/CANCELLED
        VALID_TRANSITIONS.put(PICKED, Set.of(DELIVERING, CANCELLED));
        // DELIVERING状态可流转到：DELIVERED/CANCELLED
        VALID_TRANSITIONS.put(DELIVERING, Set.of(DELIVERED, CANCELLED));
        // DELIVERED/CANCELLED为终态，不允许流转
        VALID_TRANSITIONS.put(DELIVERED, Set.of());
        VALID_TRANSITIONS.put(CANCELLED, Set.of());
    }

    WaybillStatus(String desc) {
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }
    /**
     * 校验状态流转是否合法
     * @param targetStatus 目标状态
     * @return true=合法，false=非法
     */
    public boolean canTransitionTo(WaybillStatus targetStatus) {
        return VALID_TRANSITIONS.get(this).contains(targetStatus);
    }

    /**
     * 根据状态字符串获取枚举（忽略大小写）
     * @param statusStr 状态字符串（如"CREATED"/"created"）
     * @return 对应的枚举
     * @throws IllegalArgumentException 状态不存在时抛出
     */
    public static WaybillStatus fromString(String statusStr) {
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(statusStr))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的运单状态：" + statusStr));
    }

}

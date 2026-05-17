package org.anta.enums;


public enum OrderStatus {
    PENDING,        // Khách hàng vừa đặt
    CONFIRMED,      // Admin/Shop xác nhận
    SHIPPED,        // Đang giao
    DELIVERED,      // Giao thành công
    CANCELLED   ,     // Đã hủy
    PENDING_PAYMENT,
    PAID,
    FAILED
}



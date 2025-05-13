package com.sd31.sunday.DTO;

import lombok.Data;

@Data
public class VnPayPaymentDTO {
    private long amount; // Tổng tiền sau khi tính ship và giảm giá, nhân 100 rồi mới truyền vào VNPAY
    private String orderId; // Mã đơn hàng (vnp_TxnRef)
    private String orderInfo; // Thông tin đơn hàng (vnp_OrderInfo)
    private String locale; // Ngôn ngữ (vn/en)
    private String createDate; // Thời gian tạo YYYYMMDDHHmmss
}
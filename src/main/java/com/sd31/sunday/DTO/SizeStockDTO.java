package com.sd31.sunday.DTO;

import com.sd31.sunday.model.KichCo;
import lombok.Data;

@Data
public class SizeStockDTO {
    private KichCo kichCo;
    private int soLuongTon;
    private boolean available; // Thêm trường để đánh dấu size có sẵn hay không
}

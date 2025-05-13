package com.sd31.sunday.DTO;

import com.sd31.sunday.model.MauSac;
import lombok.Data;

import java.util.List;

@Data
public class ColorSizeStockDTO {
    private MauSac mauSac;
    private List<
            SizeStockDTO> sizes;
}
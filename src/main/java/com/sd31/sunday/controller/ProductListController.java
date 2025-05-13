package com.sd31.sunday.controller;

import com.sd31.sunday.model.SanPham;
import com.sd31.sunday.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class ProductListController {

    @Autowired
    private SanPhamService sanPhamService;
    @Autowired
    private DanhMucService danhMucService;
    @Autowired
    private ThuongHieuService thuongHieuService;
    @Autowired
    private ChatLieuService chatLieuService;
    @Autowired
    private KieuDangService kieuDangService;


    @GetMapping("/bo-suu-tap")
    public String getProductListPage(
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size, // Adjust size as needed
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "danhMucId", required = false) Integer danhMucId,
            @RequestParam(name = "thuongHieuId", required = false) Integer thuongHieuId,
            @RequestParam(name = "chatLieuId", required = false) Integer chatLieuId,
            @RequestParam(name = "kieuDangId", required = false) Integer kieuDangId
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String trangThai = "Hoạt động";

        Page<SanPham> productPage = sanPhamService.getFilteredProducts(
                pageable, minPrice, maxPrice, danhMucId, thuongHieuId, chatLieuId, kieuDangId, trangThai
        );

        model.addAttribute("productPage", productPage);
        model.addAttribute("sanPhamList", productPage.getContent()); // For easier iteration in Thymeleaf

        // Filter parameters for keeping filter state after pagination/filtering
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("danhMucId", danhMucId);
        model.addAttribute("thuongHieuId", thuongHieuId);
        model.addAttribute("chatLieuId", chatLieuId);
        model.addAttribute("kieuDangId", kieuDangId);

        // Load attributes for filter dropdowns
        model.addAttribute("danhMucList", danhMucService.getAllDanhMuc());
        model.addAttribute("thuongHieuList", thuongHieuService.getAllThuongHieu());
        model.addAttribute("chatLieuList", chatLieuService.getAllChatLieu());
        model.addAttribute("kieuDangList", kieuDangService.getAllKieuDang());


        return "customer/productList";
    }
}
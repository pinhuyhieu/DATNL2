package com.sd31.sunday.controller;

import com.sd31.sunday.model.SanPham;
import com.sd31.sunday.service.SanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private SanPhamService sanPhamService;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        List<SanPham> sanPhamList = sanPhamService.getAllSanPhamsWithMinPriceAndImages();
        model.addAttribute("sanPhamList", sanPhamList);
        model.addAttribute("isSearchPage", false); // Đặt flag là false cho trang chủ mặc định
        return "customer/home";
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        return index(session, model); // Sử dụng lại method index
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam("keyword") String keyword, Model model) {
        String trangThai = "Hoạt động";
        List<SanPham> sanPhamList = sanPhamService.searchByKeyword(keyword, trangThai);
        model.addAttribute("sanPhamList", sanPhamList);
        model.addAttribute("isSearchPage", true); // Đặt flag là true cho trang tìm kiếm
        model.addAttribute("searchKeyword", keyword); // Truyền từ khóa tìm kiếm để hiển thị trong view
        return "customer/home";
    }
}
package com.sd31.sunday.controller;

import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.service.DiaChiService;
import com.sd31.sunday.service.HoaDonService;
import com.sd31.sunday.service.KhachHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangService khachHangService;

    @Autowired
    private HoaDonService hoaDonService; // Inject HoaDonService

    @Autowired
    private DiaChiService diaChiService; // Inject DiaChiService


    @GetMapping("/hien-thi")
    public String viewKhachHangList(Model model,
                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                    @RequestParam(name = "size", defaultValue = "10") int size,
                                    @RequestParam(name = "keyword", required = false) String keyword,
                                    @RequestParam(name = "trangThai", required = false) String trangThai) {
        Pageable pageable = PageRequest.of(page, size);
        Page<KhachHang> khachHangPage;

        if (keyword != null && !keyword.isEmpty()) {
            khachHangPage = khachHangService.searchKhachHang(keyword, pageable); // Cần service method search
        } else if (trangThai != null && !trangThai.isEmpty()) {
            khachHangPage = khachHangService.filterKhachHangByTrangThai(trangThai, pageable); // Cần service method filter by status
        }
        else {
            khachHangPage = khachHangService.getAllKhachHang(pageable);
        }

        model.addAttribute("khachHangPage", khachHangPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("activePage", "khachHang"); // Set activePage for Customer List
        return "admin/khach-hang/khach-hang-list";
    }

    @GetMapping("/detail/{id}")
    public String viewKhachHangDetail(@PathVariable("id") Integer id, Model model) {
        KhachHang khachHang = khachHangService.getKhachHangById(id);
        if (khachHang != null) {
            model.addAttribute("khachHang", khachHang);
            model.addAttribute("diaChiList", diaChiService.getDiaChiByKhachHangId(id)); // Lấy danh sách địa chỉ
            model.addAttribute("hoaDonList", hoaDonService.getHoaDonByKhachHangId(id)); // Lấy lịch sử đơn hàng
            model.addAttribute("activePage", "khachHang"); // Set activePage for Customer Detail
            return "admin/khach-hang/khach-hang-detail";
        } else {
            // Xử lý khi không tìm thấy khách hàng
            return "redirect:/admin/khach-hang/hien-thi"; // Hoặc trang lỗi
        }
    }

    // ... Các action khác (thêm, sửa, xóa - sau này) ...
}
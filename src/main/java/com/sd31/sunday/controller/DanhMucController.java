package com.sd31.sunday.controller;

import com.sd31.sunday.model.*;
import com.sd31.sunday.service.DanhMucService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/danh-muc")
public class DanhMucController {

    @Autowired
    private DanhMucService danhMucService;

    private MauSac createDummyMauSac() {
        MauSac dummyMauSac = new MauSac();
        dummyMauSac.setMauSacId(0);
        dummyMauSac.setTenMauSac("");
        dummyMauSac.setTrangThai("Hoạt động");
        return dummyMauSac;
    }

    private KichCo createDummyKichCo() {
        KichCo dummyKichCo = new KichCo();
        dummyKichCo.setKichCoId(0); // Or any default ID
        dummyKichCo.setTenKichCo(""); // Or any default value
        dummyKichCo.setTrangThai("Hoạt động"); // Or any default status
        return dummyKichCo;
    }

    private KieuDang createDummyKieuDang() {
        KieuDang dummyKieuDang = new KieuDang();
        dummyKieuDang.setKieuDangId(0); // Or any default ID
        dummyKieuDang.setTenKieuDang(""); // Or any default value
        dummyKieuDang.setTrangThai("Hoạt động"); // Or any default status
        return dummyKieuDang;
    }

    private ThuongHieu createDummyThuongHieu() {
        ThuongHieu dummyThuongHieu = new ThuongHieu();
        dummyThuongHieu.setThuongHieuId(0); // Or any default ID
        dummyThuongHieu.setTenThuongHieu(""); // Or any default value
        dummyThuongHieu.setTrangThai("Hoạt động"); // Or any default status
        return dummyThuongHieu;
    }

    private ChatLieu createDummyChatLieu() {
        ChatLieu dummyChatLieu = new ChatLieu();
        dummyChatLieu.setChatLieuId(0); // Or any default ID
        dummyChatLieu.setTenChatLieu(""); // Or any default value
        dummyChatLieu.setTrangThai("Hoạt động"); // Or any default status
        return dummyChatLieu;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "danhMuc");
        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
        model.addAttribute("danhMucList", danhMucList);
        model.addAttribute("danhMuc", new DanhMuc());
        model.addAttribute("isEdit", false);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        model.addAttribute("chatLieu", createDummyChatLieu()); // Add a dummy ChatLieu
        return "admin/thuoc-tinh/danh-muc";
    }

    @PostMapping("/add")
    public String addDanhMuc(@ModelAttribute("danhMuc") DanhMuc danhMuc,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (danhMuc.getTenDanhMuc() == null || danhMuc.getTenDanhMuc().trim().isEmpty()) {
            bindingResult.addError(new FieldError("danhMuc", "tenDanhMuc", "Tên danh mục không được để trống"));
            return returnWithError(model, danhMuc, false);
        }

        try {
            danhMucService.saveDanhMuc(danhMuc);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
            return "redirect:/admin/danh-muc";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên danh mục đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, danhMuc, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        DanhMuc danhMuc = danhMucService.getDanhMucById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));
        model.addAttribute("danhMuc", danhMuc);
        model.addAttribute("activePage", "danhMuc");
        model.addAttribute("isEdit", true);
        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
        model.addAttribute("danhMucList", danhMucList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        model.addAttribute("chatLieu", createDummyChatLieu()); // Add a dummy ChatLieu
        return "admin/thuoc-tinh/danh-muc";
    }

    @PostMapping("/update")
    public String updateDanhMuc(@ModelAttribute("danhMuc") DanhMuc danhMuc,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (danhMuc.getTenDanhMuc() == null || danhMuc.getTenDanhMuc().trim().isEmpty()) {
            bindingResult.addError(new FieldError("danhMuc", "tenDanhMuc", "Tên danh mục không được để trống"));
            return returnWithError(model, danhMuc, true);
        }

        try {
            danhMucService.updateDanhMuc(danhMuc);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
            return "redirect:/admin/danh-muc";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên danh mục đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, danhMuc, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            danhMucService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterDanhMuc(
            @RequestParam(value = "trangThai", required = false) String trangThai,
            @RequestParam(value = "search", required = false) String search,
            Model model,
            HttpServletRequest request) {
        
        // Log request parameters
        System.out.println("Filter request - trangThai: " + trangThai + ", search: " + search);
        
        List<DanhMuc> danhMucList;
        
        if (search != null && !search.trim().isEmpty()) {
            // Tìm kiếm theo tên
            danhMucList = danhMucService.searchByTenDanhMuc(search.trim());
            // Nếu có lọc theo trạng thái, lọc thêm kết quả tìm kiếm
            if (trangThai != null && !trangThai.isEmpty()) {
                danhMucList = danhMucList.stream()
                    .filter(dm -> dm.getTrangThai().equals(trangThai))
                    .collect(Collectors.toList());
            }
        } else if (trangThai != null && !trangThai.isEmpty()) {
            // Chỉ lọc theo trạng thái
            danhMucList = danhMucService.getDanhMucByTrangThai(trangThai);
        } else {
            // Lấy tất cả
            danhMucList = danhMucService.getAllDanhMuc();
        }
        
        // Log results
        System.out.println("Found " + danhMucList.size() + " results");
        
        model.addAttribute("danhMucList", danhMucList);
        
        // Check if this is an AJAX request
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "admin/thuoc-tinh/danh-muc :: danhMucTable";
        } else {
            return "admin/thuoc-tinh/danh-muc";
        }
    }

    private String returnWithError(Model model, DanhMuc danhMuc, boolean isEdit) {
        model.addAttribute("danhMuc", danhMuc);
        model.addAttribute("activePage", "danhMuc");
        model.addAttribute("isEdit", isEdit);
        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
        model.addAttribute("danhMucList", danhMucList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        model.addAttribute("chatLieu", createDummyChatLieu()); // Add a dummy ChatLieu
        return "admin/thuoc-tinh/danh-muc";
    }
}
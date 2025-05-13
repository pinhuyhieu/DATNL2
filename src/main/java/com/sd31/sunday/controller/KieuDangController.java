package com.sd31.sunday.controller;

import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.model.KieuDang;
import com.sd31.sunday.model.MauSac;
import com.sd31.sunday.service.KieuDangService;
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

@Controller
@RequestMapping("/admin/kieu-dang")
public class KieuDangController {

    @Autowired
    private KieuDangService kieuDangService;

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

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "kieuDang");
        List<KieuDang> kieuDangList = kieuDangService.getAllKieuDang();
        model.addAttribute("kieuDangList", kieuDangList);
        model.addAttribute("kieuDang", new KieuDang());
        model.addAttribute("isEdit", false);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        return "admin/thuoc-tinh/kieu-dang";
    }

    @PostMapping("/add")
    public String addKieuDang(@ModelAttribute("kieuDang") KieuDang kieuDang,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (kieuDang.getTenKieuDang() == null || kieuDang.getTenKieuDang().trim().isEmpty()) {
            bindingResult.addError(new FieldError("kieuDang", "tenKieuDang", "Tên kiểu dáng không được để trống"));
            return returnWithError(model, kieuDang, false);
        }

        try {
            kieuDangService.saveKieuDang(kieuDang);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm kiểu dáng thành công!");
            return "redirect:/admin/kieu-dang";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên kiểu dáng đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, kieuDang, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        KieuDang kieuDang = kieuDangService.getKieuDangById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kiểu dáng với ID: " + id));
        model.addAttribute("kieuDang", kieuDang);
        model.addAttribute("activePage", "kieuDang");
        model.addAttribute("isEdit", true);
        List<KieuDang> kieuDangList = kieuDangService.getAllKieuDang();
        model.addAttribute("kieuDangList", kieuDangList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        return "admin/thuoc-tinh/kieu-dang";
    }

    @PostMapping("/update")
    public String updateKieuDang(@ModelAttribute("kieuDang") KieuDang kieuDang,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (kieuDang.getTenKieuDang() == null || kieuDang.getTenKieuDang().trim().isEmpty()) {
            bindingResult.addError(new FieldError("kieuDang", "tenKieuDang", "Tên kiểu dáng không được để trống"));
            return returnWithError(model, kieuDang, true);
        }

        try {
            kieuDangService.updateKieuDang(kieuDang);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật kiểu dáng thành công!");
            return "redirect:/admin/kieu-dang";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên kiểu dáng đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, kieuDang, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            kieuDangService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterKieuDang(@RequestParam(value = "trangThai", required = false) String trangThai, Model model) {
        List<KieuDang> kieuDangList;
        if (trangThai != null && !trangThai.isEmpty()) {
            kieuDangList = kieuDangService.getKieuDangByTrangThai(trangThai);
        } else {
            kieuDangList = kieuDangService.getAllKieuDang();
        }
        model.addAttribute("kieuDangList", kieuDangList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        return "admin/thuoc-tinh/kieu-dang :: kieuDangTable";
    }

    private String returnWithError(Model model, KieuDang kieuDang, boolean isEdit) {
        model.addAttribute("kieuDang", kieuDang);
        model.addAttribute("activePage", "kieuDang");
        model.addAttribute("isEdit", isEdit);
        List<KieuDang> kieuDangList = kieuDangService.getAllKieuDang();
        model.addAttribute("kieuDangList", kieuDangList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        return "admin/thuoc-tinh/kieu-dang";
    }
}
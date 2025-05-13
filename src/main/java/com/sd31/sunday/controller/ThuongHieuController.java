package com.sd31.sunday.controller;

import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.model.KieuDang;
import com.sd31.sunday.model.MauSac;
import com.sd31.sunday.model.ThuongHieu;
import com.sd31.sunday.service.ThuongHieuService;
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
@RequestMapping("/admin/thuong-hieu")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuService thuongHieuService;

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

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "thuongHieu");
        List<ThuongHieu> thuongHieuList = thuongHieuService.getAllThuongHieu();
        model.addAttribute("thuongHieuList", thuongHieuList);
        model.addAttribute("thuongHieu", new ThuongHieu());
        model.addAttribute("isEdit", false);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        return "admin/thuoc-tinh/thuong-hieu";
    }

    @PostMapping("/add")
    public String addThuongHieu(@ModelAttribute("thuongHieu") ThuongHieu thuongHieu,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (thuongHieu.getTenThuongHieu() == null || thuongHieu.getTenThuongHieu().trim().isEmpty()) {
            bindingResult.addError(new FieldError("thuongHieu", "tenThuongHieu", "Tên thương hiệu không được để trống"));
            return returnWithError(model, thuongHieu, false);
        }

        try {
            thuongHieuService.saveThuongHieu(thuongHieu);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thương hiệu thành công!");
            return "redirect:/admin/thuong-hieu";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên thương hiệu đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, thuongHieu, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        ThuongHieu thuongHieu = thuongHieuService.getThuongHieuById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu với ID: " + id));
        model.addAttribute("thuongHieu", thuongHieu);
        model.addAttribute("activePage", "thuongHieu");
        model.addAttribute("isEdit", true);
        List<ThuongHieu> thuongHieuList = thuongHieuService.getAllThuongHieu();
        model.addAttribute("thuongHieuList", thuongHieuList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        return "admin/thuoc-tinh/thuong-hieu";
    }

    @PostMapping("/update")
    public String updateThuongHieu(@ModelAttribute("thuongHieu") ThuongHieu thuongHieu,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (thuongHieu.getTenThuongHieu() == null || thuongHieu.getTenThuongHieu().trim().isEmpty()) {
            bindingResult.addError(new FieldError("thuongHieu", "tenThuongHieu", "Tên thương hiệu không được để trống"));
            return returnWithError(model, thuongHieu, true);
        }

        try {
            thuongHieuService.updateThuongHieu(thuongHieu);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thương hiệu thành công!");
            return "redirect:/admin/thuong-hieu";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên thương hiệu đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, thuongHieu, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            thuongHieuService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterThuongHieu(@RequestParam(value = "trangThai", required = false) String trangThai, Model model) {
        List<ThuongHieu> thuongHieuList;
        if (trangThai != null && !trangThai.isEmpty()) {
            thuongHieuList = thuongHieuService.getThuongHieuByTrangThai(trangThai);
        } else {
            thuongHieuList = thuongHieuService.getAllThuongHieu();
        }
        model.addAttribute("thuongHieuList", thuongHieuList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        return "admin/thuoc-tinh/thuong-hieu :: thuongHieuTable";
    }

    private String returnWithError(Model model, ThuongHieu thuongHieu, boolean isEdit) {
        model.addAttribute("thuongHieu", thuongHieu);
        model.addAttribute("activePage", "thuongHieu");
        model.addAttribute("isEdit", isEdit);
        List<ThuongHieu> thuongHieuList = thuongHieuService.getAllThuongHieu();
        model.addAttribute("thuongHieuList", thuongHieuList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        return "admin/thuoc-tinh/thuong-hieu";
    }
}
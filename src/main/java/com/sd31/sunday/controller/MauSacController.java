package com.sd31.sunday.controller;

import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.model.MauSac;
import com.sd31.sunday.service.MauSacService;
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
@RequestMapping("/admin/mau-sac")
public class MauSacController {

    @Autowired
    private MauSacService mauSacService;

    private KichCo createDummyKichCo() {
        KichCo dummyKichCo = new KichCo();
        dummyKichCo.setKichCoId(0);
        dummyKichCo.setTenKichCo("");
        dummyKichCo.setTrangThai("Hoạt động");
        return dummyKichCo;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "mauSac");
        List<MauSac> mauSacList = mauSacService.getAllMauSac();
        model.addAttribute("mauSacList", mauSacList);
        model.addAttribute("mauSac", new MauSac());
        model.addAttribute("isEdit", false);
        model.addAttribute("kichCo", createDummyKichCo());
        return "admin/thuoc-tinh/mau-sac";
    }

    @PostMapping("/add")
    public String addMauSac(@ModelAttribute("mauSac") MauSac mauSac,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (mauSac.getTenMauSac() == null || mauSac.getTenMauSac().trim().isEmpty()) {
            bindingResult.addError(new FieldError("mauSac", "tenMauSac", "Tên màu sắc không được để trống"));
            return returnWithError(model, mauSac, false);
        }

        try {
            mauSacService.saveMauSac(mauSac);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm màu sắc thành công!");
            return "redirect:/admin/mau-sac";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên màu sắc đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, mauSac, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        MauSac mauSac = mauSacService.getMauSacById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Màu Sắc với ID: " + id));
        model.addAttribute("mauSac", mauSac);
        model.addAttribute("activePage", "mauSac");
        model.addAttribute("isEdit", true);
        List<MauSac> mauSacList = mauSacService.getAllMauSac();
        model.addAttribute("mauSacList", mauSacList);
        model.addAttribute("kichCo", createDummyKichCo());
        return "admin/thuoc-tinh/mau-sac";
    }

    @PostMapping("/update")
    public String updateMauSac(@ModelAttribute("mauSac") MauSac mauSac,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (mauSac.getTenMauSac() == null || mauSac.getTenMauSac().trim().isEmpty()) {
            bindingResult.addError(new FieldError("mauSac", "tenMauSac", "Tên màu sắc không được để trống"));
            return returnWithError(model, mauSac, true);
        }

        try {
            mauSacService.updateMauSac(mauSac);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật màu sắc thành công!");
            return "redirect:/admin/mau-sac";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên màu sắc đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, mauSac, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            mauSacService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterMauSac(@RequestParam(value = "trangThai", required = false) String trangThai, Model model) {
        List<MauSac> mauSacList;
        if (trangThai != null && !trangThai.isEmpty()) {
            mauSacList = mauSacService.getMauSacByTrangThai(trangThai);
        } else {
            mauSacList = mauSacService.getAllMauSac();
        }
        model.addAttribute("mauSacList", mauSacList);
        model.addAttribute("kichCo", createDummyKichCo());
        return "admin/thuoc-tinh/mau-sac :: mauSacTable"; // Return only the table fragment
    }

    private String returnWithError(Model model, MauSac mauSac, boolean isEdit) {
        model.addAttribute("mauSac", mauSac);
        model.addAttribute("activePage", "mauSac");
        model.addAttribute("isEdit", isEdit);
        List<MauSac> mauSacList = mauSacService.getAllMauSac();
        model.addAttribute("mauSacList", mauSacList);
        model.addAttribute("kichCo", createDummyKichCo());
        return "admin/thuoc-tinh/mau-sac";
    }
}
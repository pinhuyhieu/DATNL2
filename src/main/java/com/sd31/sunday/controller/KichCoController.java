package com.sd31.sunday.controller;

import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.model.MauSac;
import com.sd31.sunday.service.KichCoService;
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
@RequestMapping("/admin/kich-co")
public class KichCoController {

    @Autowired
    private KichCoService kichCoService;

    private MauSac createDummyMauSac() {
        MauSac dummyMauSac = new MauSac();
        dummyMauSac.setMauSacId(0); // Or any default ID
        dummyMauSac.setTenMauSac(""); // Or any default value
        dummyMauSac.setTrangThai("Hoạt động"); // Or any default status
        return dummyMauSac;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "kichCo");
        List<KichCo> kichCoList = kichCoService.getAllKichCo();
        model.addAttribute("kichCoList", kichCoList);
        model.addAttribute("kichCo", new KichCo());
        model.addAttribute("isEdit", false);
        model.addAttribute("mauSac", createDummyMauSac()); // Add a dummy MauSac
        return "admin/thuoc-tinh/kich-co";
    }

    @PostMapping("/add")
    public String addKichCo(@ModelAttribute("kichCo") KichCo kichCo,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (kichCo.getTenKichCo() == null || kichCo.getTenKichCo().trim().isEmpty()) {
            bindingResult.addError(new FieldError("kichCo", "tenKichCo", "Tên kích cỡ không được để trống"));
            return returnWithError(model, kichCo, false);
        }

        try {
            kichCoService.saveKichCo(kichCo);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm kích cỡ thành công!");
            return "redirect:/admin/kich-co";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên kích cỡ đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, kichCo, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        KichCo kichCo = kichCoService.getKichCoById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kích cỡ với ID: " + id));
        model.addAttribute("kichCo", kichCo);
        model.addAttribute("activePage", "kichCo");
        model.addAttribute("isEdit", true);
        List<KichCo> kichCoList = kichCoService.getAllKichCo();
        model.addAttribute("kichCoList", kichCoList);
        model.addAttribute("mauSac", createDummyMauSac()); // Add a dummy MauSac
        return "admin/thuoc-tinh/kich-co";
    }

    @PostMapping("/update")
    public String updateKichCo(@ModelAttribute("kichCo") KichCo kichCo,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (kichCo.getTenKichCo() == null || kichCo.getTenKichCo().trim().isEmpty()) {
            bindingResult.addError(new FieldError("kichCo", "tenKichCo", "Tên kích cỡ không được để trống"));
            return returnWithError(model, kichCo, true);
        }

        try {
            kichCoService.updateKichCo(kichCo);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật kích cỡ thành công!");
            return "redirect:/admin/kich-co";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên kích cỡ đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, kichCo, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            kichCoService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterKichCo(@RequestParam(value = "trangThai", required = false) String trangThai, Model model) {
        List<KichCo> kichCoList;
        if (trangThai != null && !trangThai.isEmpty()) {
            kichCoList = kichCoService.getKichCoByTrangThai(trangThai);
        } else {
            kichCoList = kichCoService.getAllKichCo();
        }
        model.addAttribute("kichCoList", kichCoList);
        model.addAttribute("mauSac", createDummyMauSac()); // Add a dummy MauSac
        return "admin/thuoc-tinh/kich-co :: kichCoTable"; // Return only the table fragment
    }

    private String returnWithError(Model model, KichCo kichCo, boolean isEdit) {
        model.addAttribute("kichCo", kichCo);
        model.addAttribute("activePage", "kichCo");
        model.addAttribute("isEdit", isEdit);
        List<KichCo> kichCoList = kichCoService.getAllKichCo();
        model.addAttribute("kichCoList", kichCoList);
        model.addAttribute("mauSac", createDummyMauSac()); // Add a dummy MauSac
        return "admin/thuoc-tinh/kich-co";
    }
}
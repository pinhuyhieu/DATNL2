package com.sd31.sunday.controller;

import com.sd31.sunday.model.*;
import com.sd31.sunday.service.ChatLieuService;
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
@RequestMapping("/admin/chat-lieu")
public class ChatLieuController {

    @Autowired
    private ChatLieuService chatLieuService;

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

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "chatLieu");
        List<ChatLieu> chatLieuList = chatLieuService.getAllChatLieu();
        model.addAttribute("chatLieuList", chatLieuList);
        model.addAttribute("chatLieu", new ChatLieu());
        model.addAttribute("isEdit", false);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        return "admin/thuoc-tinh/chat-lieu";
    }

    @PostMapping("/add")
    public String addChatLieu(@ModelAttribute("chatLieu") ChatLieu chatLieu,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (chatLieu.getTenChatLieu() == null || chatLieu.getTenChatLieu().trim().isEmpty()) {
            bindingResult.addError(new FieldError("chatLieu", "tenChatLieu", "Tên chất liệu không được để trống"));
            return returnWithError(model, chatLieu, false);
        }

        try {
            chatLieuService.saveChatLieu(chatLieu);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm chất liệu thành công!");
            return "redirect:/admin/chat-lieu";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên chất liệu đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, chatLieu, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        ChatLieu chatLieu = chatLieuService.getChatLieuById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chất liệu với ID: " + id));
        model.addAttribute("chatLieu", chatLieu);
        model.addAttribute("activePage", "chatLieu");
        model.addAttribute("isEdit", true);
        List<ChatLieu> chatLieuList = chatLieuService.getAllChatLieu();
        model.addAttribute("chatLieuList", chatLieuList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        return "admin/thuoc-tinh/chat-lieu";
    }

    @PostMapping("/update")
    public String updateChatLieu(@ModelAttribute("chatLieu") ChatLieu chatLieu,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (chatLieu.getTenChatLieu() == null || chatLieu.getTenChatLieu().trim().isEmpty()) {
            bindingResult.addError(new FieldError("chatLieu", "tenChatLieu", "Tên chất liệu không được để trống"));
            return returnWithError(model, chatLieu, true);
        }

        try {
            chatLieuService.updateChatLieu(chatLieu);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chất liệu thành công!");
            return "redirect:/admin/chat-lieu";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên chất liệu đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, chatLieu, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            chatLieuService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterChatLieu(@RequestParam(value = "trangThai", required = false) String trangThai, Model model) {
        List<ChatLieu> chatLieuList;
        if (trangThai != null && !trangThai.isEmpty()) {
            chatLieuList = chatLieuService.getChatLieuByTrangThai(trangThai);
        } else {
            chatLieuList = chatLieuService.getAllChatLieu();
        }
        model.addAttribute("chatLieuList", chatLieuList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        return "admin/thuoc-tinh/chat-lieu :: chatLieuTable";
    }

    private String returnWithError(Model model, ChatLieu chatLieu, boolean isEdit) {
        model.addAttribute("chatLieu", chatLieu);
        model.addAttribute("activePage", "chatLieu");
        model.addAttribute("isEdit", isEdit);
        List<ChatLieu> chatLieuList = chatLieuService.getAllChatLieu();
        model.addAttribute("chatLieuList", chatLieuList);
        model.addAttribute("mauSac", createDummyMauSac());
        model.addAttribute("kichCo", createDummyKichCo()); // Add a dummy KichCo
        model.addAttribute("kieuDang", createDummyKieuDang()); // Add a dummy KieuDang
        model.addAttribute("thuongHieu", createDummyThuongHieu()); // Add a dummy ThuongHieu
        return "admin/thuoc-tinh/chat-lieu";
    }
}
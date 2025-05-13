package com.sd31.sunday.controller;

import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.service.KHService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tai-khoan")
public class TaiKhoanKhachHangController {

    @Autowired
    private KHService khachHangService;

    // Đặt sẵn @ModelAttribute cho tất cả handler methods trong controller này
    @ModelAttribute("user")
    public KhachHang getLoggedInUser(HttpSession session) {
        return (KhachHang) session.getAttribute("loggedInUser");
    }

    @GetMapping("")
    public String taiKhoanPage(@ModelAttribute("user") KhachHang user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }

        // Lấy thông tin mới nhất từ database
        KhachHang khachHang = khachHangService.findById(user.getKhachHangId());
        model.addAttribute("user", khachHang);
        return "quan-ly-tai-khoan";
    }

    @PostMapping("/update-phone")
    public String updatePhone(@ModelAttribute("user") KhachHang user,
                              @RequestParam("soDienThoai") String soDienThoai,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        boolean updated = khachHangService.updateSoDienThoai(user.getKhachHangId(), soDienThoai);

        if (updated) {
            // Cập nhật lại session
            KhachHang updatedUser = khachHangService.findById(user.getKhachHangId());
            session.setAttribute("loggedInUser", updatedUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật số điện thoại thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại đã được sử dụng hoặc không hợp lệ!");
        }

        return "redirect:/tai-khoan";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute("user") KhachHang user,
                                 @RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu xác nhận không khớp!");
            return "redirect:/tai-khoan";
        }

        boolean changed = khachHangService.changePassword(user.getKhachHangId(), oldPassword, newPassword);

        if (changed) {
            // Cập nhật lại session
            KhachHang updatedUser = khachHangService.findById(user.getKhachHangId());
            session.setAttribute("loggedInUser", updatedUser);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Đổi mật khẩu thành công!");
        } else {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu cũ không chính xác!");
        }

        return "redirect:/tai-khoan";
    }
}

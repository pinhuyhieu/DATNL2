package com.sd31.sunday.controller;

import com.sd31.sunday.model.NhanVien;
import com.sd31.sunday.service.NhanVienLoginService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Import List if needed (not currently used in controller)
// import java.util.List;

@Controller
public class NhanVienLoginController {

    @Autowired
    private NhanVienLoginService nhanVienService;

    // Trang đăng nhập
    @GetMapping("/nhanvien/login")
    public String loginPage(HttpSession session, Model model) { // Add Model to handle flash attributes
        if (session.getAttribute("loggedInUser") != null) {
            // Assuming /admin/thong-ke is the correct post-login page
            return "redirect:/admin/thong-ke"; // Chuyển hướng nếu đã đăng nhập
        }
        // SweetAlert success message from registration redirect is automatically in the model as a flash attribute
        // No need to explicitly add it here, Thymeleaf/SweetAlert script on login page will find it.
        return "dangnhapnhanvien"; // Trang login nhân viên (assuming this is the Thymeleaf template name)
    }

    // Xử lý đăng nhập
    @PostMapping("/nhanvien/login")
    public String loginSubmit(@RequestParam("soDienThoai") String soDienThoai,
                              @RequestParam("password") String password,
                              HttpSession session, Model model) {
        NhanVien nhanVien = nhanVienService.login(soDienThoai, password);
        if (nhanVien != null) {
            session.setAttribute("loggedInUser", nhanVien);
            // Assuming /admin/thong-ke is the correct post-login page
            return "redirect:/admin/thong-ke"; // Chuy hướng đến trang chủ admin
        } else {
            // Add error message to model for SweetAlert
            model.addAttribute("errorMessage", "Số điện thoại hoặc mật khẩu không đúng.");
            return "dangnhapnhanvien"; // Assuming "dangnhanvien" is the Thymeleaf template name for login
        }
    }

    // Xử lý đăng xuất
    @GetMapping("/nhanvien/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        // Optional: Add a logout success message flash attribute if you want a SweetAlert on login page
        // redirectAttributes.addFlashAttribute("message", "Đăng xuất thành công!");
        return "redirect:/nhanvien/login"; // Quay lại trang đăng nhập
    }


    // Trang đăng ký nhân viên
    @GetMapping("/nhanvien/register")
    public String showRegisterPage(Model model) {
        // Check if model already contains nhanVien (e.g., after validation error redirect - though POST/redirect/GET pattern usually means a fresh model)
        // It's safer to always add it for the initial GET, or check if BindingResult errors caused a re-render
        if (!model.containsAttribute("nhanVien")) {
            model.addAttribute("nhanVien", new NhanVien()); // Initialize only if not already present (e.g., on initial GET)
        }
        // No need to add hasBindingErrors=false explicitly here, null/absence means false
        return "dangkynhanvien"; // Trang đăng ký nhân viên (assuming this is the Thymeleaf template name)
    }

    // Xử lý đăng ký
    @PostMapping("/nhanvien/register")
    public String registerUser(@ModelAttribute("nhanVien") NhanVien nhanVien,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        // Call service for validation and saving. Service adds errors to BindingResult.
        // Note: Service returns null if BindingResult has errors.
        String serviceResult = nhanVienService.dangKyNhanVien(nhanVien, result);

        if (result.hasErrors()) {
            // BindingResult errors are automatically added to the model
            // Add a flag to indicate there were binding errors for the SweetAlert script
            model.addAttribute("hasBindingErrors", true);
            // The 'nhanVien' object with populated fields and errors is also automatically in the model
            return "dangkynhanvien"; // Stay on the registration page to show field errors and generic SweetAlert
        }

        // If no BindingResult errors, check the service's specific result string
        if ("Đăng ký thành công".equals(serviceResult)) {
            // Add success message as a flash attribute for redirect
            redirectAttributes.addFlashAttribute("message", serviceResult);
            return "redirect:/nhanvien/login"; // Redirect to login on success
        } else {
            // Service returned a specific error message (e.g., phone exists)
            model.addAttribute("message", serviceResult); // Add specific error message to model for SweetAlert
            // No binding errors in this case, so no need to add hasBindingErrors = true
            // model.addAttribute("hasBindingErrors", false); // Optional, but default absence works
            return "dangkynhanvien"; // Stay on the registration page to show the specific error
        }
    }
}
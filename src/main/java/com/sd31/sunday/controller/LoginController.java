package com.sd31.sunday.controller;

import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.service.KhachHangService;
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

@Controller
public class LoginController {
    @Autowired
    private KhachHangService khachHangService;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if(session.getAttribute("loggedInUser") != null){
            return "redirect:/home"; // Đã đăng nhập, chuyển về trang chủ
        }
        model.addAttribute("user", session.getAttribute("loggedInUser")); // Pass user info for navbar (có thể null)
        return "login"; // Trang đăng nhập
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              HttpSession session, Model model) {
        KhachHang khachHang = khachHangService.login(email, password);
        if (khachHang != null) {
            session.setAttribute("loggedInUser", khachHang);
            return "redirect:/home"; // Đăng nhập thành công, chuyển về trang chủ
        } else {
            model.addAttribute("errorMessage", "Thông tin đăng nhập không đúng.");
            model.addAttribute("user", session.getAttribute("loggedInUser")); // Pass user info for navbar (có thể null)
            return "login"; // Đăng nhập thất bại, hiển thị lại trang login với lỗi
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login"; // Đăng xuất, chuyển về trang login
    }

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {
        if(session.getAttribute("loggedInUser") != null){
            return "redirect:/home"; // Đã đăng nhập, chuyển về trang chủ
        }
        model.addAttribute("khachHang", new KhachHang()); // Khởi tạo đối tượng KhachHang cho form đăng ký
        model.addAttribute("user", session.getAttribute("loggedInUser")); // Pass user info for navbar (có thể null)
        return "register"; // Trang đăng ký
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("khachHang") KhachHang khachHang,
                               BindingResult result,
                               RedirectAttributes redirectAttributes, Model model, HttpSession session) { // Thêm HttpSession session vào đây

        String registerResult = khachHangService.dangKyKhachHang(khachHang, result);

        if(result.hasErrors()){
            model.addAttribute("user", session.getAttribute("loggedInUser")); // Truyền session vào đây
            return "register"; // Đăng ký lỗi, trả lại trang đăng ký với lỗi
        }

        if (registerResult.equals("Đăng ký thành công")) {
            redirectAttributes.addFlashAttribute("message", registerResult);
            return "redirect:/login"; // Đăng ký thành công, chuyển về trang login
        }else{
            model.addAttribute("message", registerResult);
            model.addAttribute("user", session.getAttribute("loggedInUser")); // Truyền session vào đây
            return "register"; // Đăng ký lỗi khác, trả lại trang đăng ký với thông báo
        }
    }
}
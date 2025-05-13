package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

@Entity // Đánh dấu đây là một Entity
@Table(name = "nhan_vien") // Tên bảng trong database
@Data // Lombok: Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Lombok: Constructor không tham số
@AllArgsConstructor // Lombok: Constructor đầy đủ tham số
public class NhanVien {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID tự tăng
    @Column(name = "nhan_vien_id") // Tên cột
    private Integer nhanVienId;

    @NaturalId // ID tự nhiên
    @Column(name = "ma_nhan_vien", nullable = false, unique = true, length = 255) // Cột mã nhân viên
    private String maNhanVien;

    @Column(name = "ten", nullable = false, length = 255) // Cột tên
    private String ten;

    @Column(name = "mat_khau", nullable = false, length = 255) // Cột mật khẩu
    private String matKhau;

    @Column(name = "so_dien_thoai", nullable = false, unique = true, length = 20) // Cột số điện thoại
    private String soDienThoai;

    @Column(name = "trang_thai", nullable = false, length = 255) // Cột trạng thái
    private String trangThai;
}
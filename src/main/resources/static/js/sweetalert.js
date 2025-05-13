function confirmLogout() {
    Swal.fire({
        title: 'Bạn có chắc chắn muốn đăng xuất?',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#1e88e5', // Màu nút xác nhận
        cancelButtonColor: '#d33', // Màu nút hủy
        confirmButtonText: 'Đăng xuất',
        cancelButtonText: 'Hủy',
        background: '#2c2c2c', // Màu nền (tông đen)
        color: '#ffffff', // Màu chữ
        customClass: {
            popup: 'custom-popup', // Áp dụng lớp tùy chỉnh cho hộp thoại
            title: 'custom-title',
            icon: 'custom-icon',
            actions: 'custom-actions',
            confirmButton: 'custom-confirm-btn',
            cancelButton: 'custom-cancel-btn',
        },
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('logout-form').submit();
        }
    });
}




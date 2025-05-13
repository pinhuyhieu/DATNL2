

document.addEventListener("DOMContentLoaded", function () {
    var modal = document.getElementById("productModal");
    var closeButton = document.querySelector(".close-button");

    // Đóng modal khi nhấp vào nút "×"
    closeButton.addEventListener("click", function () {
        modal.style.display = "none";
    });

    // Đóng modal khi nhấp vào vùng ngoài modal
    window.addEventListener("click", function (event) {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    });
});
document.addEventListener('DOMContentLoaded', function () {
    const productCards = document.querySelectorAll('.product-card');
    const productModal = document.getElementById('productModal');
    const modalCloseButton = document.querySelector('.close-button');
    const modalAddToCartBtn = document.getElementById('modalAddToCartBtn');
    const sizeButtonsContainer = document.getElementById('sizeButtons');
    const colorButtonsContainer = document.getElementById('colorButtons');
    const productModalImage = document.getElementById('productModalImage');
    const productModalName = document.getElementById('productModalName');
    const productModalBrand = document.getElementById('productModalBrand');
    const productModalPrice = document.getElementById('productModalPrice');
    const productModalCode = document.getElementById('productModalCode');
    const hoaDonIdInput = document.getElementById('hoaDonIdInput');
    const discountCodeSelect = document.getElementById('discountCodeSelect');
    const applyDiscountBtn = document.getElementById('applyDiscountBtn');
    const finalTotalDisplay = document.getElementById('finalTotalDisplay');
    const originalTotalDisplay = document.getElementById('originalTotalDisplay'); // Lấy element hiển thị tổng tiền gốc

    console.log("DOM after update:", document.querySelectorAll('.remove-item').length, "remove buttons found");
    console.log("DOM after update:", document.querySelectorAll('.cart-quantity-input').length, "quantity inputs found");
    let currentProductId = null;
    let selectedSize = null;
    let selectedColor = null;
    let selectedChiTietSanPhamId = null;



    applyDiscountBtn.addEventListener('click', async function () {
        const maGiamGiaId = discountCodeSelect.value;
        const hoaDonId = hoaDonIdInput.value;
        const selectedDiscountName = discountCodeSelect.options[discountCodeSelect.selectedIndex].text;

        if (!maGiamGiaId) {
            Swal.fire('Chú ý!', 'Vui lòng chọn mã giảm giá.', 'warning');
            return;
        }

        try {
            const response = await fetch(`/admin/pos/apply-discount?hoaDonId=${hoaDonId}&maGiamGiaId=${maGiamGiaId}`, {
                method: 'POST'
            });

            if (!response.ok) {
                const errorData = await response.json();
                let errorMessage = 'Áp dụng mã giảm giá thất bại!';
                if (errorData && errorData.message) {
                    errorMessage = errorData.message;
                }
                Swal.fire('Lỗi!', errorMessage, 'error');
                return;
            }

            const updatedCartFragment = await response.text();
            document.getElementById('cartItemsContainer').innerHTML = updatedCartFragment;

            // Gắn lại các sự kiện
            ganSuKienXoaKhoiGio();
            ganSuKienCapNhatSoLuong();
            updateCartCount();

            // Cập nhật text hiển thị của select box
            updateDiscountSelectDisplay(selectedDiscountName);

            Swal.fire('Thành công!', 'Mã giảm giá đã được áp dụng.', 'success');

        } catch (error) {
            console.error('Lỗi khi áp dụng mã giảm giá:', error);
            Swal.fire('Lỗi!', 'Có lỗi xảy ra khi áp dụng mã giảm giá.', 'error');
        }
    });

// Thêm hàm mới để cập nhật hiển thị của select
    function updateDiscountSelectDisplay(discountName) {
        // Lấy lại select element sau khi fragment đã được cập nhật
        const discountSelect = document.getElementById('discountCodeSelect');

        // Tạo và chèn option mới vào đầu với text là tên mã giảm giá đã chọn
        const activeOption = document.createElement('option');
        activeOption.value = 'active';
        activeOption.text = `Đang áp dụng: ${discountName}`;
        activeOption.selected = true;

        // Xóa option "active" cũ nếu có
        for (let i = 0; i < discountSelect.options.length; i++) {
            if (discountSelect.options[i].value === 'active') {
                discountSelect.remove(i);
                break;
            }
        }

        // Chèn option mới vào đầu
        discountSelect.insertBefore(activeOption, discountSelect.firstChild);
    }
    async function layChiTietSanPhamId() {
        if (!selectedSize || !selectedColor || !currentProductId) return;

        const url = `/api/chi-tiet-san-pham/tim-theo-thuoc-tinh?sanPhamId=${currentProductId}&size=${selectedSize}&color=${selectedColor}`;
        try {
            const response = await fetch(url);
            if (!response.ok) {
                console.error('Lỗi gọi API layChiTietSanPhamId:', response.status, response.statusText);
                throw new Error(`Lỗi HTTP: ${response.status} - ${response.statusText}`);
            }
            const chiTiet = await response.json();
            console.log("API Response cho layChiTietSanPhamId:", chiTiet);

            if (!chiTiet || !chiTiet.chiTietSanPhamId) {
                Swal.fire('Lỗi!', 'Không tìm thấy sản phẩm với kích cỡ và màu đã chọn!', 'error');
                selectedChiTietSanPhamId = null;
            } else {
                selectedChiTietSanPhamId = chiTiet.chiTietSanPhamId;
            }
        } catch (error) {
            console.error('Lỗi layChiTietSanPhamId:', error);
            Swal.fire('Lỗi!', error.message || 'Có lỗi xảy ra khi tìm chi tiết sản phẩm.', 'error');
            selectedChiTietSanPhamId = null;
        }
    }

    modalAddToCartBtn.addEventListener('click', async function () {
        if (!selectedSize || !selectedColor) {
            Swal.fire('Thiếu thông tin!', 'Vui lòng chọn kích cỡ và màu sắc.', 'warning');
            return;
        }

        await layChiTietSanPhamId();

        if (!selectedChiTietSanPhamId) {
            Swal.fire('Lỗi!', 'Không thể thêm vào hóa đơn do không tìm thấy sản phẩm phù hợp.', 'error');
            return;
        }

        // const hoaDonId = hoaDonIdInput.value; // **ĐÃ XÓA dòng này**
        console.log("--- modalAddToCartBtn.click: hoaDonId trước khi gọi capNhatGioHang: " + hoaDonIdInput.value); // **LOG hoaDonIdInput.value**
        document.getElementById('modalHoaDonIdInput').value = hoaDonIdInput.value; // (Dòng này thừa, nhưng cứ để đó)

        try {
            const response = await fetch('/admin/pos/them-vao-gio-hang', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: new URLSearchParams({
                    chiTietSanPhamId: selectedChiTietSanPhamId,
                    hoaDonId: hoaDonIdInput.value // **Truyền hoaDonIdInput.value ở đây luôn**
                })
            });

            if (response.ok) {
                Swal.fire('Thành công!', 'Sản phẩm đã được thêm vào hóa đơn.', 'success');
                capNhatGioHang(hoaDonIdInput.value); // **Truyền hoaDonIdInput.value TRỰC TIẾP vào capNhatGioHang**
                closeModal();
            } else {
                console.error('Lỗi thêm vào hóa đơn API:', response.status, response.statusText);
                Swal.fire('Lỗi!', 'Thêm vào hóa đơn thất bại!', 'error');
            }
        } catch (error) {
            console.error('Lỗi thêm vào hóa đơn:', error);
            Swal.fire('Lỗi!', 'Không thể kết nối tới server.', 'error');
        }
    });

    function capNhatGioHang(hoaDonId) {
        console.log("--- Bắt đầu capNhatGioHang() cho hoaDonId:", hoaDonId, "---");
        const cartItemsElement = document.getElementById('cartItemsContainer');
        console.log("cartItemsElement:", cartItemsElement);

        if (!cartItemsElement) {
            console.error("!!! LỖI: Không tìm thấy phần tử cartItemsContainer trong DOM !!!");
            return;
        }

        const url = `/admin/pos/gio-hang/${hoaDonId}`; // **URL AJAX - ĐÃ KHỚP VỚI CONTROLLER, prefix '/admin/pos'**
        console.log("AJAX URL:", url);

        fetch(url)
            .then(response => {
                console.log("AJAX Response từ /admin/pos/gio-hang/${hoaDonId}:", response);
                if (!response.ok) {
                    console.error('Lỗi gọi API /admin/pos/gio-hang/${hoaDonId}:', response.status, response.statusText);
                    throw new Error(`Lỗi tải hóa đơn chi tiết: ${response.status} - ${response.statusText}`);
                }
                return response.text();
            })
            .then(html => {
                console.log("HTML fragment cartItems NHẬN ĐƯỢC (độ dài):", html.length);
                console.log("HTML fragment cartItems NHẬN ĐƯỢC:", html);

                setTimeout(function() {
                    console.log("Bắt đầu thiết lập innerHTML cho cartItemsContainer...");
                    cartItemsElement.innerHTML = html;
                    console.log("innerHTML cho cartItemsContainer đã được thiết lập.");
                    ganSuKienXoaKhoiGio();
                    ganSuKienCapNhatSoLuong();
                    updateCartCount();
                    console.log("Cập nhật hóa đơn chi tiết thành công (sau innerHTML và các hàm gắn sự kiện).");
                }, 200);

            })
            .catch(error => {
                console.error('Lỗi AJAX trong capNhatGioHang:', error);
                Swal.fire('Lỗi!', 'Không thể cập nhật hóa đơn chi tiết.', 'error');
            });
        console.log("--- Kết thúc capNhatGioHang() ---");
    }

    function updateCartCount() {
        const cartItemElements = document.querySelectorAll('#cartItemsContainer .cart-item');
        const cartCount = cartItemElements.length;
        document.getElementById('cartCount').textContent = cartCount + ' sản phẩm';
    }

    function ganSuKienXoaKhoiGio() {
        console.log("--- ganSuKienXoaKhoiGio() ---");
        document.querySelectorAll('.remove-item').forEach(button => {
            button.onclick = function () {
                const chiTietHoaDonId = button.getAttribute('data-cart-item-id');
                const hoaDonId = button.getAttribute('data-hoa-don-id');
                console.log("Nút xóa sản phẩm được click, chiTietHoaDonId:", chiTietHoaDonId, "hoaDonId:", hoaDonId);
                xoaSanPhamKhoiGio(chiTietHoaDonId, hoaDonId);
            };
        });
        console.log("--- ganSuKienXoaKhoiGio() hoàn tất ---");
    }

    function xoaSanPhamKhoiGio(chiTietHoaDonId, hoaDonId) {
        console.log("Bắt đầu xoaSanPhamKhoiGio() với chiTietHoaDonId:", chiTietHoaDonId, "hoaDonId:", hoaDonId);

        fetch(`/admin/pos/gio-hang/xoa-san-pham?chiTietHoaDonId=${encodeURIComponent(chiTietHoaDonId)}&hoaDonId=${encodeURIComponent(hoaDonId)}`, { // **ĐƯỜNG DẪN ĐÃ KHỚP VỚI CONTROLLER, prefix '/admin/pos'**
            method: 'POST'
        })
            .then(response => {
                console.log("xoaSanPhamKhoiGio() - AJAX response:", response); // **LOG RESPONSE**
                if (!response.ok) {
                    console.error('Lỗi gọi API /admin/pos/gio-hang/xoa-san-pham:', response.status, response.statusText);
                    throw new Error(`Lỗi xóa sản phẩm khỏi hóa đơn: ${response.status} - ${response.statusText}`);
                }
                return response.text();
            })
            .then(html => {
                console.log("HTML fragment cartItems nhận được sau xóa:", html);
                capNhatGioHang(hoaDonId);
                console.log("Gọi capNhatGioHang() thành công sau xóa");
            })
            .catch(error => {
                console.error('Lỗi xoaSanPhamKhoiGio:', error);
                Swal.fire('Lỗi!', 'Không thể xóa sản phẩm khỏi hóa đơn.', 'error');
            });
        console.log("--- Kết thúc xoaSanPhamKhoiGio() ---");
    }
    function ganSuKienCapNhatSoLuong() {
        console.log("--- ganSuKienCapNhatSoLuong() ---");
        document.querySelectorAll('.cart-quantity-input').forEach(input => {
            input.addEventListener('change', function () {
                const chiTietHoaDonId = input.getAttribute('data-cart-item-id');
                const hoaDonId = input.getAttribute('data-hoa-don-id');
                const soLuongMoi = input.value;
                console.log("Input số lượng thay đổi, chiTietHoaDonId:", chiTietHoaDonId, "hoaDonId:", hoaDonId, "soLuongMoi:", soLuongMoi);
                capNhatSoLuongSanPham(chiTietHoaDonId, soLuongMoi, hoaDonId);
            });
        });
        console.log("--- ganSuKienCapNhatSoLuong() hoàn tất ---");
    }

    function capNhatSoLuongSanPham(chiTietHoaDonId, soLuong, hoaDonId) {
        console.log("Bắt đầu capNhatSoLuongSanPham() với chiTietHoaDonId:", chiTietHoaDonId, "soLuong:", soLuong, "hoaDonId:", hoaDonId);
        fetch(`/admin/pos/gio-hang/cap-nhat-so-luong`, { // **ĐƯỜNG DẪN ĐÃ KHỚP VỚI CONTROLLER, prefix '/admin/pos'**
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({
                chiTietHoaDonId: chiTietHoaDonId,
                soLuong: soLuong,
                hoaDonId: hoaDonId
            })
        })
            .then(response => {
                if (!response.ok) {
                    console.error('Lỗi cập nhật số lượng:', response.status, response.statusText);
                }
                capNhatGioHang(hoaDonId);
            })
            .catch(error => {
                console.error('Lỗi cập nhật số lượng:', error);
            });
        console.log("--- Kết thúc capNhatSoLuongSanPham() ---");
    }


    function closeModal() {
        productModal.style.display = 'none';
        sizeButtonsContainer.innerHTML = '';
        colorButtonsContainer.innerHTML = '';
        selectedSize = null;
        selectedColor = null;
        currentProductId = null;
        selectedChiTietSanPhamId = null;
    }

    function dinhDangTien(amount) {
        return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(amount);
    }

    productCards.forEach(card => {
        card.onclick = async function () {
            currentProductId = card.getAttribute('data-product-id');
            await openModal(card);
        };
    });


    async function openModal(card) {
        productModal.style.display = 'block';
        productModalImage.src = card.querySelector('.product-image').src;
        productModalName.textContent = card.querySelector('.product-name').textContent;
        productModalBrand.textContent = card.querySelector('.product-brand').textContent;
        productModalPrice.textContent = card.querySelector('.product-price').textContent;
        productModalCode.textContent = card.querySelector('.product-code').textContent;
        document.getElementById('modalHoaDonIdInput').value = hoaDonIdInput.value;

        await loadSizesAndColors(currentProductId);
    }


    async function loadSizesAndColors(productId) {
        sizeButtonsContainer.innerHTML = 'Đang tải kích cỡ...';
        colorButtonsContainer.innerHTML = 'Đang tải màu sắc...';
        try {
            const sizeResponse = await fetch(`/admin/pos/api/sizes?productId=${productId}`);
            if (!sizeResponse.ok) {
                console.error('Lỗi gọi API /admin/pos/api/sizes:', sizeResponse.status, sizeResponse.statusText);
                sizeButtonsContainer.innerHTML = 'Lỗi tải kích cỡ.';
                throw new Error(`Lỗi tải kích cỡ: ${sizeResponse.status} - ${sizeResponse.statusText}`);
            }
            const sizes = await sizeResponse.json();
            console.log("API Response cho /admin/pos/api/sizes:", sizes);

            const colorResponse = await fetch(`/admin/pos/api/colors?productId=${productId}`);
            if (!colorResponse.ok) {
                console.error('Lỗi gọi API /admin/pos/api/colors:', colorResponse.status, colorResponse.statusText);
                colorButtonsContainer.innerHTML = 'Lỗi tải màu sắc.';
                throw new Error(`Lỗi tải màu sắc: ${colorResponse.status} - ${colorResponse.statusText}`);
            }
            const colors = await colorResponse.json();
            console.log("API Response cho /admin/pos/api/colors:", colors);

            sizeButtonsContainer.innerHTML = sizes.map(size =>
                `<button type="button" class="option-button size-btn" data-size="${size}">${size}</button>`
            ).join('');

            colorButtonsContainer.innerHTML = colors.map(color =>
                `<button type="button" class="option-button color-btn" data-color="${color}">${color}</button>`
            ).join('');

            document.querySelectorAll('.size-btn').forEach(btn => {
                btn.onclick = function () {
                    document.querySelectorAll('.size-btn').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    selectedSize = btn.getAttribute('data-size');
                    document.getElementById('selectedSizeText').textContent = selectedSize;
                };
            });

            document.querySelectorAll('.color-btn').forEach(btn => {
                btn.onclick = function () {
                    document.querySelectorAll('.color-btn').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    selectedColor = btn.getAttribute('data-color');
                    document.getElementById('selectedColorText').textContent = selectedColor;
                };
            });

        } catch (error) {
            console.error('Lỗi loadSizesAndColors:', error);
        }
    }

    // Initial cart load on page load
    const initialHoaDonId = hoaDonIdInput.value;
    capNhatGioHang(initialHoaDonId);
    ganSuKienXoaKhoiGio();
    ganSuKienCapNhatSoLuong();
});

function chonVoucher(element) {
    var maGiamGiaId = $(element).data('id');
    var hoaDonId = $("#hoaDonId").val(); // Lấy ID hóa đơn từ input ẩn hoặc biến lưu trữ
    var tenMaGiamGia = $(element).closest("tr").find("td:nth-child(2)").text();

    console.log("Chọn mã giảm giá:", maGiamGiaId, "cho hóa đơn:", hoaDonId);

    $.ajax({
        url: "/admin/pos/apply-discount",
        type: "POST",
        data: {
            hoaDonId: hoaDonId,
            maGiamGiaId: maGiamGiaId
        },
        success: function (response) {
            var errorMessage = $(response).find("#errorMessage").text().trim();
            if (errorMessage) {
                Swal.fire({
                    title: "Lỗi!",
                    text: errorMessage,
                    icon: "error",
                    confirmButtonText: "OK"
                });
                return;
            }
            // Cập nhật giao diện giỏ hàng
            $("#discountInput").val(tenMaGiamGia);
            $("#cartContainer").html(response);
            var finalTotal = $(response).find("#finalTotalDisplay").text().trim();
            $("#finalTotalDisplay").text(finalTotal);

            Swal.fire({
                title: "Thành công!",
                text: "Mã giảm giá đã được áp dụng!",
                icon: "success",
                confirmButtonText: "OK"
            }).then(() => {
                document.getElementById("discountModal").style.display = "none";
            });
        },
        error: function (xhr, status, error) {
            console.error("Lỗi khi gọi AJAX:", error);
            try {
                const errorResponse = JSON.parse(xhr.responseText);
                Swal.fire({
                    title: "Lỗi!",
                    text: errorResponse.error || "Có lỗi xảy ra!",
                    icon: "error",
                    confirmButtonText: "OK"
                });
            } catch (e) {
                Swal.fire({
                    title: "Lỗi!",
                    text: "Lỗi không xác định, vui lòng thử lại!",
                    icon: "error",
                    confirmButtonText: "OK"
                });
            }
        }
    });
}

function updateProductList() {
    let keyword = document.getElementById("searchsanpham").value;

    if (!keyword) {
        keyword = '';
    }

    if (keyword.trim() === '') {
        document.getElementById('product-list-container').innerHTML = initialProductList;
        return;
    }
    fetch(`http://localhost:8080/admin/pos/search?keyword=${encodeURIComponent(keyword)}`)
        .then(response => response.json())
        .then(data => {
            console.log("Dữ liệu trả về từ API:", data);
            updateProductListUI(data);
        })
        .catch(error => {
            console.error('Lỗi danh sách sản phẩm:', error);
        });
}

function updateProductListUI(products) {
    const productsGrid = document.getElementById('productsGrid');
    productsGrid.innerHTML = ''; // Xóa danh sách cũ

    if (products.length === 0) {
        productsGrid.innerHTML = '<p>Không có sản phẩm phù hợp</p>';
        return;
    }

    console.log("🔥 Tổng số sản phẩm từ API:", products.length);

    let uniqueProducts = {}; // Khai báo biến lưu sản phẩm duy nhất

    products.forEach((item, index) => {
        console.log(`🛒 Sản phẩm ${index + 1}:`, item);

        // Xử lý lỗi dữ liệu đầu vào
        const parts = item.split(/\s*-\s*/); // Tách dữ liệu
        if (parts.length < 7) {
            console.error(`❌ Lỗi dữ liệu sản phẩm ${index + 1}:`, item);
            return;
        }

        const spct = {
            id: parts[0], // sanPhamId
            tensp: parts[1],
            tenChatLieu: parts[2],
            soluongsanpham: parts[3],
            maSanPham: parts[4],
            thuongHieu: parts[5] || "Không xác định",
            hinhanh: parts.slice(6, parts.length - 2).join(' - '),
        };

        console.log("Chi tiết sản phẩm:", spct);
        console.log("Đường dẫn ảnh:", spct.hinhanh);
        uniqueProducts[spct.id] = spct;
    });

    const productList = Object.values(uniqueProducts);

    productList.forEach(spct => {
        const productCard = document.createElement('div');
        productCard.classList.add('product-card');
        productCard.setAttribute('data-product-id', spct.id);
        productCard.innerHTML = `
            <div class="product-image-container">
                 <img src="${spct.hinhanh}" alt="Product Image" class="product-image">
            </div>
            <div class="product-details">
                <div class="product-name">${spct.tensp}</div>
                <div class="product-brand">${spct.thuongHieu}</div>
                <div class="product-code">Mã SP: ${spct.maSanPham}</div>
            </div>
        `;

        productsGrid.appendChild(productCard);
    });

    console.log("🛍️ Số sản phẩm hiển thị:", document.querySelectorAll('.product-card').length);
}




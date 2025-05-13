let timeout;
$(document).on('input', '#mavoucher-input', function () {
    clearTimeout(timeout);
    var voucherId = $(this).val();
    if (voucherId.trim() === "") {
        console.log("Trống voucher")
        return;
    }
    timeout = setTimeout(function () {
        nhapvoucher(voucherId);
    }, 1500);
});

function nhapvoucher(voucherId) {
    console.log("Voucher ID: ", voucherId);
    $.ajax({
        url: "/admin/pos/selectvc",
        type: "GET",
        data: {
            id: voucherId
        },
        success: function (response) {
            $("#mavoucher-input").val(response.mavocher);
            $("#idvoucher-input").val(response.idvoucher);
            $("#mucgiam-input").val(response.mucgiam);
            $("#mucgiam").text(response.mucgiam);
            if(!response.thanhtien || response.thanhtien === ".00"  ){
                $("#thanhtien").text('Thành tiền: ' + 0 +' VND');
            }else{
                $("#thanhtien").text('Thành tiền: ' + response.thanhtien +' VND');
            }
            $("#thanhTien").val(response.thanhTien);
            $('#voucherModal').modal('hide');
        },
        error: function (xhr, status, error) {
            const errorResponse = JSON.parse(xhr.responseText);
            console.error("Lỗi : ", errorResponse.error);
            if (errorResponse.error) {
                Swal.fire({
                    title: 'Thông báo',
                    text:errorResponse.error,
                    icon: 'error',
                    confirmButtonText: 'OK'
                }).finally(() => {
                    location.reload();
                });
                // location.reload()
            }
        }
    });
}

function chonVoucher(element) {
    var voucherId = $(element).data('id');
    console.log("Voucher ID: ", voucherId);
    $.ajax({
        url: "/admin/pos/selectvc",
        type: "GET",
        data: {
            id: voucherId
        },
        success: function (response) {
            $("#mavoucher-input").val(response.mavocher);
            $("#idvoucher-input").val(response.idvoucher);
            $("#mucgiam-input").val(response.mucgiam);
            $("#mucgiam").text(response.mucgiam);
            // $("#mucgiam").text(response.loai);
            if(!response.thanhtien || response.thanhtien === ".00"  ){
                $("#thanhtien").text('Thành tiền: ' + 0 +' VND');
            }else{
                $("#thanhtien").text('Thành tiền: ' + response.thanhtien +' VND');
            }
            $("#thanhTien").val(response.thanhTien);
            $("#thanhtien-input").val(response.thanhTien);
            $('#voucherModal').modal('hide');
        },
        error: function (xhr, status, error) {
            console.error("Lỗi khi gọi AJAX:", error);
            const errorResponse = JSON.parse(xhr.responseText);
            if (errorResponse.error) {
                // alert(errorResponse.error);
                Swal.fire({
                    title: 'Thông báo',
                    text: errorResponse.error,
                    icon: 'error',
                    confirmButtonText: 'OK'
                })
            }
        }
    });
}

function chonkh(element) {
    var idkh = $(element).data('id');
    console.log("kh ID: ", idkh);
    $.ajax({
        url: "/admin/pos/chonkh",
        type: "GET",
        data: {
            id: idkh
        },
        success: function (response) {
            $("#tenkh-input").val(response.tenkh);
            $("#idkh-input").val(response.id);
            $("#sdt-input").val(response.sdt);
            $("#searchBox").val(response.diachi);

            // $("#selectedProvince").val(response.idTinhThanhPho);
            // $("#selectedDistrict").val(response.idQuanHuyen);
            // $("#selectedWard").val(response.idXaPhuong);

            // $("#tentinh").val(response.tentinh);
            // $("#tenhuyen").val(response.tenhuyen);
            // $("#tenxa").val(response.tenxa);

            $('#chonkhachhangModal').modal('hide');
        },
        error: function (xhr, status, error) {
            console.error("Lỗi khi gọi AJAX:", error);
            const errorResponse = JSON.parse(xhr.responseText);
            if (errorResponse.error) {
                Swal.fire({
                    title: 'Thông báo',
                    text:errorResponse,
                    icon: 'error',
                    confirmButtonText: 'OK'
                });
            }
        }
    });
}


// function chonkh(element) {
//     var idkh = $(element).data('id');
//     localStorage.removeItem("selectedCustomer");
//     axios.get(`/admin/pos/chonkh?id=${idkh}`)
//         .then(response => {
//             const data = response.data;
//             console.log("Địa chỉ : ", data);
//             localStorage.setItem("selectedCustomer", JSON.stringify({
//                 idTinhThanhPho: data.idTinhThanhPho,
//                 idQuanHuyen: data.idQuanHuyen,
//                 idXaPhuong: data.idXaPhuong
//             }));
//
//             $("#province").val(data.idTinhThanhPho).trigger("change");
//             // Gọi API để load Quận/Huyện theo Tỉnh/Thành phố
//             callApiDistrict(`https://provinces.open-api.vn/api/p/${data.idTinhThanhPho}?depth=2`)
//                 .then(() => {
//                     // Cập nhật giá trị Quận/Huyện
//                     $("#district").val(data.idQuanHuyen).trigger("change");
//
//                     // Gọi API để load Xã/Phường theo Quận/Huyện
//                     callApiWard(`https://provinces.open-api.vn/api/d/${data.idQuanHuyen}?depth=2`)
//                         .then(() => {
//                             // Cập nhật giá trị Xã/Phường
//                             $("#ward").val(data.idXaPhuong);
//                         });
//                 });
//             $('#chonkhachhangModal').modal('hide');
//         })
//         .catch(error => {
//             console.error("Error", error);
//         });
// }


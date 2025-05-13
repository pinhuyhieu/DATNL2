package com.sd31.sunday.controller;

import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.repository.HoaDonRepository;
import com.sd31.sunday.repository.KhachHangRepository;
import com.sd31.sunday.repository.SanPhamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin/thong-ke")
public class ThongKeController {

    private static final Logger logger = LoggerFactory.getLogger(ThongKeController.class);

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    // Constants
    private static final String TRANG_THAI_HOAN_THANH = "Đã giao hàng"; // Online orders typically end here
    private static final String TRANG_THAI_DA_THANH_TOAN = "Đã thanh toán"; // In-store orders often end here, or paid online orders
    private static final String TRANG_THAI_SP_HOAT_DONG = "Hoạt động"; // Status for SanPham entity count
    private static final String TRANG_THAI_CTSP_HOATDONG = "Hoạt động"; // Status for ChiTietSanPham low stock filter
    // List of statuses considered for revenue calculation (used in multiple places)
    private static final List<String> REVENUE_STATUSES = List.of(TRANG_THAI_HOAN_THANH, TRANG_THAI_DA_THANH_TOAN);
    // Constants for sales channels
    private static final String KENH_BAN_HANG_ONLINE = "Website";
    private static final String KENH_BAN_HANG_POS = "POS";


    private static final int TOP_N = 5; // For top lists
    private static final int LOW_STOCK_THRESHOLD = 20; // Low stock threshold (<=)
    private static final int LOW_STOCK_LIMIT = 10;   // Max items in low stock table

    // Date Formatters
    private static final DateTimeFormatter MONTH_YEAR_PARSER = DateTimeFormatter.ofPattern("[M/yyyy][MM/yyyy]");
    private static final DateTimeFormatter MONTH_YEAR_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("M/yyyy");
    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @GetMapping
    public String thongKe(Model model,
                          @RequestParam(name = "loaiThongKe", required = false, defaultValue = "thang") String loaiThongKe,
                          @RequestParam(name = "thangNamInput", required = false) String reqThangNamInput,
                          @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reqStartDate,
                          @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reqEndDate,
                          @RequestParam(name = "yearChart", required = false) Integer yearChartParam) {

        logger.info("Processing statistics request - Filter Type: {}, Month Input: {}, Start Date: {}, End Date: {}, Chart Year: {}",
                loaiThongKe, reqThangNamInput, reqStartDate, reqEndDate, yearChartParam);

        model.addAttribute("activePage", "thong-ke");
        int currentYear = Year.now().getValue();
        YearMonth currentYearMonth = YearMonth.now();

        // --- Process Filters ---
        // Use LocalDateTime for filter start and end dates that will be used in queries
        LocalDateTime filterStartDateTime = null;
        LocalDateTime filterEndDateTime = null;

        // Keep LocalDate variables for view display purposes
        LocalDate selectedStartDateForView = null;
        LocalDate selectedEndDateForView = null;
        String selectedThangNamForView = currentYearMonth.format(MONTH_YEAR_DISPLAY_FORMATTER);

        String filterDescription = "";

        try {
            switch (loaiThongKe) {
                case "thang":
                    YearMonth selectedYearMonth = currentYearMonth;
                    if (reqThangNamInput != null && !reqThangNamInput.trim().isEmpty()) {
                        try { selectedYearMonth = YearMonth.parse(reqThangNamInput.trim(), MONTH_YEAR_PARSER); }
                        catch (DateTimeParseException e) { logger.warn("Invalid 'thangNamInput': {}. Defaulting to current month.", reqThangNamInput, e); }
                    }
                    LocalDate firstDayOfMonth = selectedYearMonth.atDay(1);
                    LocalDate lastDayOfMonth = selectedYearMonth.atEndOfMonth();

                    filterStartDateTime = firstDayOfMonth.atStartOfDay(); // Start of the first day
                    filterEndDateTime = lastDayOfMonth.atTime(LocalTime.MAX); // End of the last day (23:59:59.999999999)

                    selectedThangNamForView = selectedYearMonth.format(MONTH_YEAR_DISPLAY_FORMATTER);
                    filterDescription = String.format("Tháng %s", selectedThangNamForView);
                    // For 'thang' filter, the date inputs are disabled, so no need to set selectedStartDateForView/selectedEndDateForView
                    break;

                case "ngay":
                    // Use reqStartDate and reqEndDate for view initial population and input validation
                    selectedStartDateForView = reqStartDate;
                    selectedEndDateForView = reqEndDate;

                    LocalDate startDate = reqStartDate;
                    LocalDate endDate = reqEndDate;

                    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                        // Swap dates if start is after end (for query and display)
                        LocalDate temp = startDate; startDate = endDate; endDate = temp;
                        selectedStartDateForView = startDate; selectedEndDateForView = endDate;
                    }

                    // Convert LocalDate to LocalDateTime for the query
                    if (startDate != null) {
                        filterStartDateTime = startDate.atStartOfDay(); // Start of the selected start day
                    }
                    if (endDate != null) {
                        filterEndDateTime = endDate.atTime(LocalTime.MAX); // End of the selected end day
                    }

                    if (startDate != null && endDate != null) { filterDescription = String.format("Từ %s đến %s", startDate.format(DATE_DISPLAY_FORMATTER), endDate.format(DATE_DISPLAY_FORMATTER)); }
                    else if (startDate != null) { filterDescription = String.format("Từ ngày %s", startDate.format(DATE_DISPLAY_FORMATTER)); }
                    else if (endDate != null){ filterDescription = String.format("Đến ngày %s", endDate.format(DATE_DISPLAY_FORMATTER)); }
                    else { filterDescription = "Theo khoảng ngày (Chưa chọn)"; }

                    // Clear month input view value when date range is selected
                    selectedThangNamForView = null;
                    break;

                default:
                    // Default to current month
                    loaiThongKe = "thang";
                    YearMonth selectedYearMonthDefault = currentYearMonth;
                    LocalDate firstDayOfMonthDefault = selectedYearMonthDefault.atDay(1);
                    LocalDate lastDayOfMonthDefault = selectedYearMonthDefault.atEndOfMonth();

                    filterStartDateTime = firstDayOfMonthDefault.atStartOfDay();
                    filterEndDateTime = lastDayOfMonthDefault.atTime(LocalTime.MAX);

                    selectedThangNamForView = selectedYearMonthDefault.format(MONTH_YEAR_DISPLAY_FORMATTER);
                    filterDescription = String.format("Tháng %s (Mặc định)", selectedThangNamForView);
                    selectedStartDateForView = null;
                    selectedEndDateForView = null;
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing filter parameters. Defaulting to current month.", e);
            loaiThongKe = "thang";
            YearMonth selectedYearMonthDefault = currentYearMonth;
            LocalDate firstDayOfMonthDefault = selectedYearMonthDefault.atDay(1);
            LocalDate lastDayOfMonthDefault = selectedYearMonthDefault.atEndOfMonth();

            filterStartDateTime = firstDayOfMonthDefault.atStartOfDay();
            filterEndDateTime = lastDayOfMonthDefault.atTime(LocalTime.MAX);

            selectedThangNamForView = selectedYearMonthDefault.format(MONTH_YEAR_DISPLAY_FORMATTER);
            filterDescription = String.format("Tháng %s (Mặc định do lỗi)", selectedThangNamForView);
            selectedStartDateForView = null;
            selectedEndDateForView = null;
            model.addAttribute("errorMessage", "Lỗi xử lý bộ lọc, đã quay về mặc định.");
        }

        // --- Add Filter Info to Model ---
        model.addAttribute("selectedLoaiThongKe", loaiThongKe);
        model.addAttribute("selectedThangNamInputForView", selectedThangNamForView);
        model.addAttribute("selectedStartDateForView", selectedStartDateForView); // Still pass LocalDate for date picker
        model.addAttribute("selectedEndDateForView", selectedEndDateForView);   // Still pass LocalDate for date picker
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("filterDescription", filterDescription);

        // --- Fetch Overall Stats (includes both online and POS completed/paid) ---
        try {
            List<String> overallRevenueStatuses = List.of(TRANG_THAI_HOAN_THANH, TRANG_THAI_DA_THANH_TOAN);
            model.addAttribute("tongDoanhThuTongThe", hoaDonRepository.findTongDoanhThuTongThe(overallRevenueStatuses));
            model.addAttribute("tongDonHangTongThe", hoaDonRepository.countHoaDonTongThe(overallRevenueStatuses));
            model.addAttribute("tongSanPhamHoatDong", sanPhamRepository.countActiveSanPham(TRANG_THAI_SP_HOAT_DONG));
            model.addAttribute("tongKhachHangTongThe", khachHangRepository.count());

        } catch (Exception e) {
            logger.error("Error fetching overall statistics.", e);
            model.addAttribute("tongDoanhThuTongThe", BigDecimal.ZERO);
            model.addAttribute("tongSanPhamHoatDong", 0L);
            model.addAttribute("tongDonHangTongThe", 0L);
            model.addAttribute("tongKhachHangTongThe", 0L);
            appendErrorMessage(model, "Lỗi tải dữ liệu tổng thể.");
        }

        // --- Fetch Channel-Specific Stats (Overall, not filtered by date range) ---
        try {
            List<String> channelRevenueStatuses = List.of(TRANG_THAI_HOAN_THANH, TRANG_THAI_DA_THANH_TOAN);
            model.addAttribute("tongDoanhThuOnline", hoaDonRepository.findTongDoanhThuByKenhAndTrangThaiIn(KENH_BAN_HANG_ONLINE, channelRevenueStatuses));
            model.addAttribute("tongDonHangOnline", hoaDonRepository.countHoaDonByKenhAndTrangThaiIn(KENH_BAN_HANG_ONLINE, channelRevenueStatuses));
            model.addAttribute("tongDoanhThuPOS", hoaDonRepository.findTongDoanhThuByKenhAndTrangThaiIn(KENH_BAN_HANG_POS, channelRevenueStatuses));
            model.addAttribute("tongDonHangPOS", hoaDonRepository.countHoaDonByKenhAndTrangThaiIn(KENH_BAN_HANG_POS, channelRevenueStatuses));
        } catch (Exception e) {
            logger.error("Error fetching channel-specific statistics.", e);
            model.addAttribute("tongDoanhThuOnline", BigDecimal.ZERO);
            model.addAttribute("tongDonHangOnline", 0L);
            model.addAttribute("tongDoanhThuPOS", BigDecimal.ZERO);
            model.addAttribute("tongDonHangPOS", 0L);
            appendErrorMessage(model, "Lỗi tải dữ liệu thống kê theo kênh bán hàng.");
        }


        // --- Fetch Line Chart Data ---
        int selectedYearForChart = (yearChartParam == null) ? currentYear : yearChartParam;
        model.addAttribute("selectedYearForChart", selectedYearForChart);
        // Cập nhật tiêu đề biểu đồ cho rõ ràng hơn
        String chartTitle = String.format("Doanh thu hoàn thành theo tháng năm %d", selectedYearForChart);
        model.addAttribute("chartTitle", chartTitle);
        try {
            List<String> chartLabels = IntStream.rangeClosed(1, 12).mapToObj(m -> "Th " + m).collect(Collectors.toList());
            // Gọi phương thức đã sửa đổi với danh sách trạng thái REVENUE_STATUSES
            List<Object[]> monthlyRevenueDataRaw = hoaDonRepository.findDoanhThuTungThangTrongNam(selectedYearForChart, REVENUE_STATUSES);
            Map<Integer, BigDecimal> revenueMap = monthlyRevenueDataRaw.stream().collect(Collectors.toMap(d -> (Integer) d[0], d -> d[1] != null ? (BigDecimal) d[1] : BigDecimal.ZERO));
            List<BigDecimal> chartData = IntStream.rangeClosed(1, 12).mapToObj(month -> revenueMap.getOrDefault(month, BigDecimal.ZERO)).collect(Collectors.toList());
            model.addAttribute("chartLabels", chartLabels);
            model.addAttribute("chartData", chartData);
        } catch (Exception e) {
            logger.error("Error fetching line chart data.", e);
            model.addAttribute("chartLabels", Collections.emptyList());
            model.addAttribute("chartData", Collections.emptyList());
            model.addAttribute("chartErrorMessage", "Lỗi tải dữ liệu biểu đồ đường.");
        }

        // --- Fetch Pie Chart Data ---
        String pieChartTitle = String.format("Trạng thái đơn hàng (%s)", filterDescription);
        model.addAttribute("pieChartTitle", pieChartTitle);
        try {
            // The pie chart should show counts for ALL statuses within the DATE/TIME range
            // Pass the calculated LocalDateTime range here
            List<Object[]> trangThaiCounts = hoaDonRepository.countByTrangThaiAndDateRange(filterStartDateTime, filterEndDateTime); // Pass LocalDateTime
            List<String> pieChartLabels = new ArrayList<>();
            List<Long> pieChartData = new ArrayList<>();
            if (trangThaiCounts != null) {
                for (Object[] row : trangThaiCounts) {
                    if (row.length >= 2 && row[0] instanceof String && row[1] instanceof Long) {
                        pieChartLabels.add((String) row[0]); pieChartData.add((Long) row[1]);
                    }
                }
            }
            model.addAttribute("pieChartLabels", pieChartLabels);
            model.addAttribute("pieChartData", pieChartData);
            logger.info("Pie chart data fetched: Labels={}, Data={}", pieChartLabels, pieChartData);
        } catch (Exception e) {
            logger.error("Error fetching pie chart data.", e);
            model.addAttribute("pieChartLabels", Collections.emptyList());
            model.addAttribute("pieChartData", Collections.emptyList());
            model.addAttribute("pieChartErrorMessage", "Lỗi tải dữ liệu biểu đồ tròn.");
        }


        // --- Fetch Filtered Stats & Tops ---
        // Filtered stats (Doanh thu, Lợi nhuận) and Top SP/KH should now use BOTH statuses
        try {
            List<String> filteredRevenueStatuses = List.of(TRANG_THAI_HOAN_THANH, TRANG_THAI_DA_THANH_TOAN); // Use this list
            Pageable top5Pageable = PageRequest.of(0, TOP_N);

            // Use the list of statuses AND the calculated LocalDateTime range
            // Ensure repository methods accept LocalDateTime for date parameters
            BigDecimal tongDoanhThuLoc = hoaDonRepository.findTongDoanhThuByDateRange(filteredRevenueStatuses, filterStartDateTime, filterEndDateTime); // Pass LocalDateTime
            BigDecimal tongLoiNhuan = hoaDonRepository.findTongLoiNhuanByDateRange(filteredRevenueStatuses, filterStartDateTime, filterEndDateTime); // Pass LocalDateTime
            List<Object[]> topSanPhamBanChay = sanPhamRepository.findTopSanPhamBanChayByDateRange(filteredRevenueStatuses, filterStartDateTime, filterEndDateTime, top5Pageable); // Pass LocalDateTime
            List<Object[]> topKhachHang = khachHangRepository.findTopKhachHangMuaNhieuNhat(filteredRevenueStatuses, filterStartDateTime, filterEndDateTime, top5Pageable); // Pass LocalDateTime

            model.addAttribute("tongDoanhThuLoc", tongDoanhThuLoc);
            model.addAttribute("tongLoiNhuan", tongLoiNhuan);
            model.addAttribute("topSanPhamBanChay", topSanPhamBanChay);
            model.addAttribute("topKhachHang", topKhachHang);
        } catch (Exception e) {
            logger.error("Error fetching filtered stats/tops.", e);
            model.addAttribute("tongDoanhThuLoc", BigDecimal.ZERO);
            model.addAttribute("tongLoiNhuan", BigDecimal.ZERO);
            model.addAttribute("topSanPhamBanChay", Collections.emptyList());
            model.addAttribute("topKhachHang", Collections.emptyList());
            appendErrorMessage(model, "Lỗi tải dữ liệu top (SP/KH) theo bộ lọc.");
        }

        // --- Fetch Low Stock Products ---
        try {
            Pageable lowStockPageable = PageRequest.of(0, LOW_STOCK_LIMIT);
            List<ChiTietSanPham> sanPhamSapHetHang = sanPhamRepository.findSanPhamSapHetHang(
                    LOW_STOCK_THRESHOLD,
                    TRANG_THAI_CTSP_HOATDONG,
                    lowStockPageable
            );
            model.addAttribute("sanPhamSapHetHang", sanPhamSapHetHang);
            model.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
        } catch (Exception e) {
            logger.error("Error fetching low stock products.", e);
            model.addAttribute("sanPhamSapHetHang", Collections.emptyList());
            model.addAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
            appendErrorMessage(model, "Lỗi tải SP sắp hết hàng.");
        }

        return "thong-ke";
    }

    private void appendErrorMessage(Model model, String message) {
        if (model != null) {
            String current = (String) model.getAttribute("errorMessage");
            model.addAttribute("errorMessage", (current != null ? current + "; " : "") + message);
        }
    }
}
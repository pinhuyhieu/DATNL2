package com.sd31.sunday.service;

import com.sd31.sunday.model.ChiTietSanPham;
// Import the repository for ChiTietHoaDon
import com.sd31.sunday.repository.ChiTietHoaDonRepository;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import jakarta.transaction.Transactional; // Use jakarta.transaction.Transactional
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional; // Import Optional

@Service
public class ChiTietSanPhamService {

    private static final Logger logger = LoggerFactory.getLogger(ChiTietSanPhamService.class);

    // --- Define the exact status string for pending orders ---
    // --- MAKE SURE this matches the value used in your HoaDon entity ---
    private static final String PENDING_PAYMENT_STATUS = "Chờ thanh toán";

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    // --- Inject the ChiTietHoaDonRepository ---
    @Autowired
    private ChiTietHoaDonRepository chiTietHoaDonRepository;

    /**
     * Retrieves all ChiTietSanPham entities.
     * @return A list of all ChiTietSanPham.
     */
    public List<ChiTietSanPham> getAllChiTietSanPhams() {
        return chiTietSanPhamRepository.findAll();
    }

    /**
     * Finds all ChiTietSanPham entities associated with a specific SanPham ID.
     * @param sanPhamId The ID of the parent SanPham.
     * @return A list of matching ChiTietSanPham.
     */
    public List<ChiTietSanPham> findBySanPhamId(Integer sanPhamId) {
        return chiTietSanPhamRepository.findBySanPham_SanPhamId(sanPhamId);
    }

    /**
     * Retrieves a specific ChiTietSanPham by its ID.
     * @param id The ID of the ChiTietSanPham.
     * @return The ChiTietSanPham entity, or null if not found.
     */
    public ChiTietSanPham getChiTietSanPhamById(Integer id) {
        return chiTietSanPhamRepository.findById(id).orElse(null);
    }

    /**
     * Saves or updates a ChiTietSanPham entity.
     * Performs validation, checks for duplicates, and updates related pending
     * ChiTietHoaDon records if the selling price (giaBan) changes during an update.
     *
     * @param chiTietSanPham The ChiTietSanPham entity to save or update.
     * @return The saved or updated ChiTietSanPham entity.
     * @throws IllegalArgumentException if validation fails or a duplicate exists.
     * @throws IllegalStateException if trying to update a non-existent record.
     */
    @Transactional // Ensures atomicity for saving CTSP and potentially updating CTHD
    public ChiTietSanPham save(ChiTietSanPham chiTietSanPham) {
        logger.debug("Attempting to save/update ChiTietSanPham: {}", chiTietSanPham);

        // --- Basic Input Validations ---
        if (chiTietSanPham.getSanPham() == null || chiTietSanPham.getSanPham().getSanPhamId() == null) {
            throw new IllegalArgumentException("Sản phẩm không được để trống.");
        }
        if (chiTietSanPham.getMauSac() == null || chiTietSanPham.getMauSac().getMauSacId() == null) {
            throw new IllegalArgumentException("Màu sắc không được để trống.");
        }
        if (chiTietSanPham.getKichCo() == null || chiTietSanPham.getKichCo().getKichCoId() == null) {
            throw new IllegalArgumentException("Kích cỡ không được để trống.");
        }
        if (chiTietSanPham.getGiaNhap() == null || chiTietSanPham.getGiaNhap().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá nhập phải là số dương và lớn hơn 0.");
        }
        if (chiTietSanPham.getGiaBan() == null || chiTietSanPham.getGiaBan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải là số dương và lớn hơn 0.");
        }
        if (chiTietSanPham.getGiaBan().compareTo(chiTietSanPham.getGiaNhap()) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn giá nhập.");
        }
        if (chiTietSanPham.getSoLuongTon() == null || chiTietSanPham.getSoLuongTon() < 0) {
            throw new IllegalArgumentException("Số lượng tồn không được âm.");
        }


        // --- Set Status based on Stock if Status is Null/Empty ---
        if (chiTietSanPham.getTrangThai() == null || chiTietSanPham.getTrangThai().trim().isEmpty()) {
            logger.debug("TrangThai is null or empty, setting based on SoLuongTon: {}", chiTietSanPham.getSoLuongTon());
            if (chiTietSanPham.getSoLuongTon() != null && chiTietSanPham.getSoLuongTon() > 0) {
                chiTietSanPham.setTrangThai("Hoạt động");
            } else {
                chiTietSanPham.setTrangThai("Không hoạt động");
            }
            logger.debug("Set TrangThai to: {}", chiTietSanPham.getTrangThai());
        }

        // --- Check for Duplicates and Prepare for Potential Price Update ---
        boolean isDuplicate;
        boolean isUpdate = chiTietSanPham.getChiTietSanPhamId() != null; // Check if it's an update
        BigDecimal oldPrice = null; // Variable to store the old price if it's an update

        if (isUpdate) {
            // --- Case: UPDATE ---
            logger.debug("Processing UPDATE for ChiTietSanPham ID: {}", chiTietSanPham.getChiTietSanPhamId());
            // Get the existing entity to compare the price *before* saving changes
            Optional<ChiTietSanPham> existingCtspOpt = chiTietSanPhamRepository.findById(chiTietSanPham.getChiTietSanPhamId());
            if (existingCtspOpt.isPresent()) {
                oldPrice = existingCtspOpt.get().getGiaBan(); // Store the current price
                logger.debug("Found existing CTSP. Old price (giaBan): {}", oldPrice);
            } else {
                // This should ideally not happen if the ID is valid from the form/request
                logger.error("Attempting to update a non-existent ChiTietSanPham with ID: {}", chiTietSanPham.getChiTietSanPhamId());
                throw new IllegalStateException("Không tìm thấy chi tiết sản phẩm để cập nhật với ID: " + chiTietSanPham.getChiTietSanPhamId());
            }

            // Check for duplicates, excluding the entity itself
            isDuplicate = chiTietSanPhamRepository.existsBySanPham_SanPhamIdAndMauSac_MauSacIdAndKichCo_KichCoIdAndChiTietSanPhamIdNot(
                    chiTietSanPham.getSanPham().getSanPhamId(),
                    chiTietSanPham.getMauSac().getMauSacId(),
                    chiTietSanPham.getKichCo().getKichCoId(),
                    chiTietSanPham.getChiTietSanPhamId() // Exclude itself
            );
            logger.debug("Duplicate check (excluding self) result: {}", isDuplicate);
        } else {
            // --- Case: NEW ---
            logger.debug("Processing NEW ChiTietSanPham");
            // Check for duplicates among all existing entities
            isDuplicate = chiTietSanPhamRepository.existsBySanPham_SanPhamIdAndMauSac_MauSacIdAndKichCo_KichCoId(
                    chiTietSanPham.getSanPham().getSanPhamId(),
                    chiTietSanPham.getMauSac().getMauSacId(),
                    chiTietSanPham.getKichCo().getKichCoId()
            );
            logger.debug("Duplicate check (for new) result: {}", isDuplicate);
        }

        if (isDuplicate) {
            logger.warn("Duplicate found for SanPham ID: {}, MauSac ID: {}, KichCo ID: {}",
                    chiTietSanPham.getSanPham().getSanPhamId(),
                    chiTietSanPham.getMauSac().getMauSacId(),
                    chiTietSanPham.getKichCo().getKichCoId());
            throw new IllegalArgumentException("Chi tiết sản phẩm với Màu sắc và Kích cỡ này đã tồn tại cho Sản phẩm này.");
        }

        // --- Save ChiTietSanPham ---
        ChiTietSanPham savedCtsp = chiTietSanPhamRepository.save(chiTietSanPham);
        logger.info("Successfully saved/updated ChiTietSanPham with ID: {}", savedCtsp.getChiTietSanPhamId());

        // --- *** UPDATE ChiTietHoaDon IF PRICE CHANGED ON UPDATE *** ---
        if (isUpdate) {
            BigDecimal newPrice = savedCtsp.getGiaBan();
            // Check if the price actually changed (using compareTo for BigDecimal)
            // Also ensures oldPrice was successfully fetched
            if (oldPrice != null && newPrice != null && newPrice.compareTo(oldPrice) != 0) {
                logger.info("Selling price (giaBan) changed for ChiTietSanPham ID: {}. Old: {}, New: {}. Triggering update for pending order items.",
                        savedCtsp.getChiTietSanPhamId(), oldPrice, newPrice);

                // Call the repository method to update related ChiTietHoaDon
                int updatedCount = chiTietHoaDonRepository.updatePriceForPendingOrderItems(
                        savedCtsp.getChiTietSanPhamId(),
                        newPrice,
                        PENDING_PAYMENT_STATUS // Use the constant status defined above
                );

                logger.info("Updated price for {} ChiTietHoaDon entries linked to CTSP ID {} in orders with status '{}'.",
                        updatedCount, savedCtsp.getChiTietSanPhamId(), PENDING_PAYMENT_STATUS);

                // --- OPTIONAL: Update HoaDon.tongTien ---
                // This is more complex as it requires recalculating the *entire* HoaDon total.
                // It might be better handled by a separate scheduled task, a trigger,
                // or recalculated on demand when viewing the HoaDon,
                // rather than doing it synchronously here for potentially many orders.
                // If you *must* do it here, you'd need to:
                // 1. Find all distinct HoaDon IDs affected by the CTHD update.
                // 2. Loop through each affected HoaDon ID.
                // 3. Fetch all CTHD for that HoaDon.
                // 4. Sum their new `tongTien` values.
                // 5. Add shipping fees, subtract discounts etc.
                // 6. Update the `HoaDon.tongTien`.
                // This adds significant complexity and potential performance impact.

            } else if (oldPrice != null && newPrice != null && newPrice.compareTo(oldPrice) == 0) {
                logger.info("Price (giaBan) did not change for ChiTietSanPham ID: {}. No update needed for pending order items.", savedCtsp.getChiTietSanPhamId());
            } else {
                logger.warn("Could not compare prices for ChiTietSanPham ID: {}. Old price: {}, New price: {}. Skipping order item update.",
                        savedCtsp.getChiTietSanPhamId(), oldPrice, newPrice);
            }
        }
        // --- *** END UPDATE ChiTietHoaDon *** ---

        return savedCtsp; // Return the saved/updated entity
    }

    /**
     * Deletes a ChiTietSanPham by its ID.
     * Note: Consider implications for existing orders before enabling deletion.
     * It might be safer to mark as inactive instead.
     *
     * @param id The ID of the ChiTietSanPham to delete.
     * @throws org.springframework.dao.EmptyResultDataAccessException if the ID doesn't exist.
     */
    @Transactional
    public void delete(Integer id) {
        // Consider adding checks here: Prevent deletion if the CTSP exists in
        // active or processing orders. For now, it just deletes.
        if (!chiTietSanPhamRepository.existsById(id)) {
            logger.warn("Attempted to delete non-existent ChiTietSanPham with ID: {}", id);
            // Consider throwing a custom exception or just logging
            return; // Or throw new EntityNotFoundException(...)
        }
        logger.warn("Deleting ChiTietSanPham with ID: {}", id);
        // You might want to check dependencies first (e.g., are there CTHD pointing to this?)
        // List<ChiTietHoaDon> relatedHoaDons = chiTietHoaDonRepository.findByChiTietSanPham_ChiTietSanPhamId(id);
        // if (!relatedHoaDons.isEmpty()) { throw new IllegalStateException("Cannot delete: referenced by order items."); }
        chiTietSanPhamRepository.deleteById(id);
        logger.info("Successfully deleted ChiTietSanPham with ID: {}", id);
    }

    /**
     * Toggles the status ('Hoạt động' / 'Không hoạt động') of a ChiTietSanPham.
     *
     * @param id The ID of the ChiTietSanPham to toggle.
     * @return The updated ChiTietSanPham entity.
     * @throws IllegalArgumentException if the ChiTietSanPham with the given ID is not found.
     */
    @Transactional
    public ChiTietSanPham toggleStatus(Integer id) {
        logger.debug("Attempting to toggle status for ChiTietSanPham ID: {}", id);
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Cannot toggle status: Chi tiết sản phẩm không tồn tại với ID: {}", id);
                    return new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với ID: " + id);
                });

        String currentStatus = ctsp.getTrangThai();
        // Robust check for status toggle
        String newStatus = "Hoạt động".equalsIgnoreCase(currentStatus) ? "Không hoạt động" : "Hoạt động";
        ctsp.setTrangThai(newStatus);

        logger.info("Toggling status for ChiTietSanPham ID: {} from '{}' to '{}'", id, currentStatus, newStatus);
        // Use the main save method to ensure consistency if other logic is added there later
        // However, be mindful this 'save' call will re-run validations/checks.
        // If performance is critical and only status changes, a direct save might be slightly faster,
        // but using the main 'save' is often safer for consistency.
        // For now, let's just save directly as price isn't changing here.
        ChiTietSanPham updatedCtsp = chiTietSanPhamRepository.save(ctsp);
        // ChiTietSanPham updatedCtsp = this.save(ctsp); // Alternative: Use the main save method

        logger.info("Successfully toggled status for ChiTietSanPham ID: {}. New status: {}", id, updatedCtsp.getTrangThai());
        return updatedCtsp;
    }
}
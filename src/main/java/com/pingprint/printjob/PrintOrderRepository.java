package com.pingprint.printjob;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface PrintOrderRepository extends JpaRepository<PrintOrder, UUID> {
	Optional<PrintOrder> findByRazorpayOrderId(String razorpayOrderId);
	Optional<PrintOrder> findByIdAndUser_Id(UUID id, UUID userId);
}

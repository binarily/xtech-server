package dk.comiu.server.payments;

import dk.comiu.server.db.Device;
import dk.comiu.server.db.Order;

import java.util.Optional;

public interface PaymentProcessor {
    Optional<Order> initiatePayment(Order order, Device device);
    Optional<Order> cancelPayment(Order order, Device device);
    boolean checkPaymentStatus(Order order, Device device);
}

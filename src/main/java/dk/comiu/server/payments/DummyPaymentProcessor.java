package dk.comiu.server.payments;

import dk.comiu.server.OrderService;
import dk.comiu.server.db.Device;
import dk.comiu.server.db.DeviceRepository;
import dk.comiu.server.db.Order;
import dk.comiu.server.db.OrderRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

// This dummy payment processor is meant to allow the developer to check their flow
// without having to implement real-life payment processor first
@Service
public class DummyPaymentProcessor implements PaymentProcessor {
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final Device serverDevice;

    public DummyPaymentProcessor(OrderRepository orderRepository,
                                 @Lazy OrderService orderService,
                                 DeviceRepository deviceRepository) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.serverDevice = deviceRepository.findFirstByType(Device.DeviceType.SERVER);
    }

    @Override
    public Optional<Order> initiatePayment(Order order, Device device) {
        switch (order.getState()) {
            case CREATED:
            case REJECTED_BY_PAYMENT_PROCESSOR:
                order.setState(Order.OrderState.SENT_TO_PAYMENT_PROCESSOR);
                order.setUpdatedBy(device);
                order.setUpdated(LocalDateTime.now());
                return Optional.of(orderRepository.save(order));
            default:
                return Optional.empty();
        }
    }

    @Override
    public Optional<Order> cancelPayment(Order order, Device device) {
        switch (order.getState()) {
            case CREATED:
            case REJECTED_BY_PAYMENT_PROCESSOR:
            case SENT_TO_PAYMENT_PROCESSOR:
                order.setState(Order.OrderState.CANCELLED);
                order.setUpdatedBy(device);
                order.setUpdated(LocalDateTime.now());
                return Optional.of(orderRepository.save(order));
            default:
                return Optional.empty();
        }
    }

    @Override
    public boolean checkPaymentStatus(Order order, Device device) {
        return order.getState() == Order.OrderState.ACCEPTED_BY_PAYMENT_PROCESSOR;
    }

    //Helper functions for testing
    public boolean setPaymentAsAccepted(Order order) {
        if (order.getState() != Order.OrderState.SENT_TO_PAYMENT_PROCESSOR) {
            return false;
        }
        order.setState(Order.OrderState.ACCEPTED_BY_PAYMENT_PROCESSOR);
        order.setUpdatedBy(serverDevice);
        order.setUpdated(LocalDateTime.now());
        order = orderRepository.save(order);

        return orderService.markOrderAsPreparing(order.getId(), serverDevice);
    }

    public boolean setPaymentAsRejected(Order order) {
        if (order.getState() != Order.OrderState.SENT_TO_PAYMENT_PROCESSOR) {
            return false;
        }
        order.setState(Order.OrderState.REJECTED_BY_PAYMENT_PROCESSOR);
        order.setUpdatedBy(serverDevice);
        order.setUpdated(LocalDateTime.now());
        orderRepository.save(order);

        return orderService.markOrderAsRejected(order.getId(), serverDevice);
    }
}

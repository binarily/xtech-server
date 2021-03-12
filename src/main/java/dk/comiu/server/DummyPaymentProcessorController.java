package dk.comiu.server;

import dk.comiu.server.db.DeviceRepository;
import dk.comiu.server.db.Order;
import dk.comiu.server.db.OrderRepository;
import dk.comiu.server.payments.DummyPaymentProcessor;
import dk.comiu.server.requests.OrderId;
import dk.comiu.server.requests.OrderRequest;
import dk.comiu.server.requests.OrderStatus;
import dk.comiu.server.requests.OrderStatusResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class DummyPaymentProcessorController {
    private final DummyPaymentProcessor dummyPaymentProcessor;
    private final OrderRepository orderRepository;

    public DummyPaymentProcessorController(DummyPaymentProcessor dummyPaymentProcessor,
                                           OrderRepository orderRepository) {
        this.dummyPaymentProcessor = dummyPaymentProcessor;
        this.orderRepository = orderRepository;
    }

    //USE ONLY FOR TESTING
    @PostMapping("/payment/ok")
    public OrderStatusResponse ok(@RequestBody OrderId request) {
        OrderStatus status = OrderStatus.OK;
        if(!LoadDatabase.DEV_SWITCH) {
            status = OrderStatus.UNAUTHORIZED;
            return new OrderStatusResponse(request, status);
        }
        Optional<Order> order = orderRepository.findById(request.id);
        if(order.isEmpty()) {
            status = OrderStatus.INTERNAL_ERROR;
        } else if(!dummyPaymentProcessor.setPaymentAsAccepted(order.get())) {
            status = OrderStatus.INTERNAL_ERROR;
        }
        return new OrderStatusResponse(request, status);
    }

    //USE ONLY FOR TESTING
    @PostMapping("/payment/nope")
    public OrderStatusResponse nope(@RequestBody OrderId request) {
        OrderStatus status = OrderStatus.OK;
        if(!LoadDatabase.DEV_SWITCH) {
            status = OrderStatus.UNAUTHORIZED;
            return new OrderStatusResponse(request, status);
        }
        Optional<Order> order = orderRepository.findById(request.id);
        if(order.isEmpty()) {
            status = OrderStatus.INTERNAL_ERROR;
        } else if(!dummyPaymentProcessor.setPaymentAsRejected(order.get())) {
            status = OrderStatus.INTERNAL_ERROR;
        }
        return new OrderStatusResponse(request, status);
    }
}
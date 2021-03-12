package dk.comiu.server;

import dk.comiu.server.db.Device;
import dk.comiu.server.db.DeviceRepository;
import dk.comiu.server.requests.*;
import io.vavr.control.Either;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class OrderController {
    private final OrderService orderService;
    private final DeviceRepository deviceRepository;

    public OrderController(OrderService orderService,
                           DeviceRepository deviceRepository) {
        this.orderService = orderService;
        this.deviceRepository = deviceRepository;
    }

    //Used by table device
    @PostMapping("/order/new")
    public OrderStatusResponse order(@RequestHeader long deviceId, @RequestBody OrderRequest request) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return new OrderStatusResponse(null, OrderStatus.INTERNAL_ERROR);
        }
        Device device = deviceOpt.get();

        Either<OrderStatus, OrderId> orderResult = orderService.createOrder(request.elements, device);
        return orderResult.fold(error -> new OrderStatusResponse(null, error),
                orderId -> new OrderStatusResponse(orderId, OrderStatus.OK));
    }

    @PostMapping("/order/retry")
    public OrderStatusResponse retry(@RequestHeader long deviceId, @RequestBody OrderId request) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return new OrderStatusResponse(null, OrderStatus.INTERNAL_ERROR);
        }
        Device device = deviceOpt.get();

        OrderStatus status = orderService.retryOrder(request, device);
        return new OrderStatusResponse(request, status);
    }

    @PostMapping("/order/cancel")
    public OrderStatusResponse cancel(@RequestHeader long deviceId, @RequestBody OrderId request) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return new OrderStatusResponse(null, OrderStatus.INTERNAL_ERROR);
        }
        Device device = deviceOpt.get();
        OrderStatus status = orderService.cancelOrder(request, device);
        return new OrderStatusResponse(request, status);
    }

    //Used by counter device
    @PostMapping("/order/ready")
    public OrderColourResponse ready(@RequestHeader long deviceId, @RequestBody OrderId request) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return new OrderColourResponse(null, OrderStatus.INTERNAL_ERROR);
        }
        Device device = deviceOpt.get();
        Either<OrderStatus, OrderColourResponse.OrderColour> readyResult
                = orderService.markOrderAsReady(request, device);
        return readyResult.fold(error -> new OrderColourResponse(request, error, null),
                colour -> new OrderColourResponse(request, OrderStatus.OK, colour));
    }

    @PostMapping("/order/done")
    public OrderStatusResponse done(@RequestHeader long deviceId, @RequestBody OrderId request) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return new OrderStatusResponse(null, OrderStatus.INTERNAL_ERROR);
        }
        Device device = deviceOpt.get();
        OrderStatus status = orderService.markOrderAsDone(request, device);
        return new OrderStatusResponse(request, status);
    }
}
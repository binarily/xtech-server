package dk.comiu.server;

import dk.comiu.server.db.Device;
import dk.comiu.server.db.Order;
import dk.comiu.server.db.OrderRepository;
import dk.comiu.server.db.SkuRepository;
import dk.comiu.server.payments.DummyPaymentProcessor;
import dk.comiu.server.payments.PaymentProcessor;
import dk.comiu.server.requests.OrderColourResponse;
import dk.comiu.server.requests.OrderId;
import dk.comiu.server.requests.OrderRequest;
import dk.comiu.server.requests.OrderStatus;
import dk.comiu.server.templates.CounterClientService;
import dk.comiu.server.templates.TableClientService;
import io.vavr.control.Either;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final SkuRepository skuRepository;
    private final PaymentProcessor paymentProcessor;
    private final SegmentService segmentService;
    private final TableClientService tableClientService;
    private final CounterClientService counterClientService;

    public OrderService(OrderRepository orderRepository,
                        SkuRepository skuRepository,
                        SegmentService segmentService,
                        DummyPaymentProcessor paymentProcessor,
                        TableClientService tableClientService,
                        CounterClientService counterClientService) {
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
        this.paymentProcessor = paymentProcessor;
        this.segmentService = segmentService;
        this.tableClientService = tableClientService;
        this.counterClientService = counterClientService;
    }

    public Either<OrderStatus, OrderId> createOrder(List<OrderRequest.Elements> elements, Device device) {
        //TODO: better error handling (than just querying SKUs twice and checking size)
        List<Order.OrderPart> skus = elements.stream()
                .takeWhile(part -> skuRepository.findById(part.sku).isPresent())
                .map(part -> new Order.OrderPart(skuRepository.findById(part.sku).get(), part.quantity))
                .collect(Collectors.toList());
        if (skus.size() != elements.size()) {
            return Left(OrderStatus.INTERNAL_ERROR);
        }
        Order newOrder = Order.fromRequest(skus, device);
        newOrder = orderRepository.save(newOrder);
        Optional<Order> orderOpt = paymentProcessor.initiatePayment(newOrder, device);
        if (orderOpt.isEmpty()) {
            return Left(OrderStatus.INTERNAL_ERROR);
        }
        return Right(OrderId.fromLong(orderOpt.get().getId()));
    }

    public OrderStatus retryOrder(OrderId orderId, Device device) {
        Optional<Order> orderOpt = orderRepository.findById(orderId.id);
        if (orderOpt.isEmpty()) {
            return OrderStatus.INTERNAL_ERROR;
        }
        Order currentOrder = orderOpt.get();

        if(device.getId() != currentOrder.getCreatedBy().getId()){
            return OrderStatus.UNAUTHORIZED;
        }

        if (currentOrder.getState() != Order.OrderState.REJECTED_BY_PAYMENT_PROCESSOR) {
            return OrderStatus.INCORRECT_STATE;
        }

        orderOpt = paymentProcessor.initiatePayment(currentOrder, device);
        return orderOpt.isPresent() ? OrderStatus.OK : OrderStatus.INTERNAL_ERROR;
    }

    public OrderStatus cancelOrder(OrderId orderId, Device device) {
        Optional<Order> orderOpt = orderRepository.findById(orderId.id);
        if (orderOpt.isEmpty()) {
            return OrderStatus.INTERNAL_ERROR;
        }
        Order currentOrder = orderOpt.get();

        if(device.getId() != currentOrder.getCreatedBy().getId()){
            return OrderStatus.UNAUTHORIZED;
        }

        switch (currentOrder.getState()) {
            case CREATED:
            case REJECTED_BY_PAYMENT_PROCESSOR:
            case SENT_TO_PAYMENT_PROCESSOR:
                orderOpt = paymentProcessor.cancelPayment(currentOrder, device);
                break;
            default:
                return OrderStatus.INCORRECT_STATE;
        }

        return orderOpt.isPresent() ? OrderStatus.OK : OrderStatus.INTERNAL_ERROR;
    }

    public Either<OrderStatus, OrderColourResponse.OrderColour> markOrderAsReady(OrderId orderId, Device device) {
        if(device.getType() != Device.DeviceType.COUNTER) {
            return Left(OrderStatus.UNAUTHORIZED);
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId.id);
        if (orderOpt.isEmpty()) {
            return Left(OrderStatus.INTERNAL_ERROR);
        }
        Order currentOrder = orderOpt.get();

        if (currentOrder.getState() != Order.OrderState.PREPARING) {
            return Left(OrderStatus.INCORRECT_STATE);
        }

        orderOpt = segmentService.assignSegment(currentOrder, device);
        if (orderOpt.isEmpty()) {
            return Left(OrderStatus.NO_SEGMENT_AVAILABLE);
        }
        currentOrder = orderOpt.get();

        currentOrder.setState(Order.OrderState.READY);
        currentOrder.setUpdatedBy(device);
        currentOrder.setUpdated(LocalDateTime.now());
        orderRepository.save(currentOrder);

        OrderStatus status = tableClientService.sendOrderAsReady(currentOrder);
        if(status != OrderStatus.OK) {
            return Left(status);
        }
        return Right(
                new OrderColourResponse.OrderColour(
                        currentOrder.getColour().getId(),
                        currentOrder.getColour().getSegment().getId()
                )
        );
    }

    public OrderStatus markOrderAsDone(OrderId orderId, Device device) {
        if(device.getType() != Device.DeviceType.COUNTER) {
            return OrderStatus.UNAUTHORIZED;
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId.id);
        if (orderOpt.isEmpty()) {
            return OrderStatus.INTERNAL_ERROR;
        }
        Order currentOrder = orderOpt.get();

        if (currentOrder.getState() != Order.OrderState.READY) {
            return OrderStatus.INCORRECT_STATE;
        }
        currentOrder.setState(Order.OrderState.DONE);
        currentOrder.setUpdatedBy(device);
        currentOrder.setUpdated(LocalDateTime.now());
        orderRepository.save(currentOrder);

        currentOrder = segmentService.freeColour(currentOrder);

        return tableClientService.sendOrderAsDone(currentOrder);
    }

    //Internal methods used by, e.g., payment processors
    public boolean markOrderAsPreparing(long orderId, Device device) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }
        Order currentOrder = orderOpt.get();

        if (currentOrder.getState() != Order.OrderState.ACCEPTED_BY_PAYMENT_PROCESSOR) {
            return false;
        }

        currentOrder.setState(Order.OrderState.PREPARING);
        currentOrder.setUpdated(LocalDateTime.now());
        currentOrder.setUpdatedBy(device);
        orderRepository.save(currentOrder);

        OrderStatus status = counterClientService.sendOrder(currentOrder);
        if(status != OrderStatus.OK) {
            return false;
        }

        status = tableClientService.sendOrderAsAccepted(currentOrder);
        return status == OrderStatus.OK;
    }

    public boolean markOrderAsRejected(long orderId, Device device) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }
        Order currentOrder = orderOpt.get();

        if (currentOrder.getState() != Order.OrderState.REJECTED_BY_PAYMENT_PROCESSOR) {
            return false;
        }

        OrderStatus status = tableClientService.sendOrderAsRejected(currentOrder);
        return status == OrderStatus.OK;
    }
}

package dk.comiu.server.templates;

import dk.comiu.server.db.Device;
import dk.comiu.server.db.DeviceRepository;
import dk.comiu.server.db.Order;
import dk.comiu.server.requests.OrderColourResponse;
import dk.comiu.server.requests.OrderId;
import dk.comiu.server.requests.OrderStatus;
import dk.comiu.server.requests.OrderStatusResponse;
import lombok.Getter;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class TableClientService {
    @Getter
    private final RestTemplate restTemplate;
    private final DeviceRepository deviceRepository;

    public TableClientService(RestTemplateBuilder restTemplateBuilder,
                              DeviceRepository deviceRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.deviceRepository = deviceRepository;
    }

    public OrderStatus sendOrderAsAccepted(Order order) {
        return sendOrderStatus(order, "accepted");
    }

    public OrderStatus sendOrderAsRejected(Order order) {
        return sendOrderStatus(order, "rejected");
    }

    public OrderStatus sendOrderAsDone(Order order) {
        return sendOrderStatus(order, "done");
    }

    public OrderStatus sendOrderAsReady(Order order) {
        Device serverDevice = deviceRepository.findFirstByType(Device.DeviceType.SERVER);

        String url = order.getCreatedBy().getCallbackUrl() + "/order/ready";

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `content-type` header
        headers.setContentType(MediaType.APPLICATION_JSON);
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("deviceId", String.valueOf(serverDevice.getId()));

        // create a post object
        OrderColourResponse post = OrderColourResponse.fromDatabase(order);

        // build the request
        HttpEntity<OrderStatusResponse> entity = new HttpEntity<>(post, headers);

        // send POST request
        OrderStatusResponse response = restTemplate.postForObject(url, entity, OrderStatusResponse.class);
        if(response == null) {
            return OrderStatus.INTERNAL_ERROR;
        }
        return response.status;
    }

    private OrderStatus sendOrderStatus(Order order, String endpoint) {
        Device serverDevice = deviceRepository.findFirstByType(Device.DeviceType.SERVER);
        String url = order.getCreatedBy().getCallbackUrl() + "/order/" + endpoint;

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `content-type` header
        headers.setContentType(MediaType.APPLICATION_JSON);
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("deviceId", String.valueOf(serverDevice.getId()));

        // create a post object
        OrderStatusResponse post = new OrderStatusResponse(OrderId.fromLong(order.getId()), OrderStatus.OK);

        // build the request
        HttpEntity<OrderStatusResponse> entity = new HttpEntity<>(post, headers);

        // send POST request
        OrderStatusResponse response = restTemplate.postForObject(url, entity, OrderStatusResponse.class);
        if(response == null) {
            return OrderStatus.INTERNAL_ERROR;
        }
        return response.status;
    }
}

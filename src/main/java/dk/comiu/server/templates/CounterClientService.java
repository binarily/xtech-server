package dk.comiu.server.templates;

import dk.comiu.server.db.Device;
import dk.comiu.server.db.DeviceRepository;
import dk.comiu.server.db.Order;
import dk.comiu.server.requests.OrderPreparationRequest;
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
public class CounterClientService {
    @Getter
    private final RestTemplate restTemplate;
    private final DeviceRepository deviceRepository;

    public CounterClientService(RestTemplateBuilder restTemplateBuilder,
                                DeviceRepository deviceRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.deviceRepository = deviceRepository;
    }

    public OrderStatus sendOrder(Order order) {
        Device serverDevice = deviceRepository.findFirstByType(Device.DeviceType.SERVER);
        Device counterDevice = deviceRepository.findFirstByType(Device.DeviceType.COUNTER);

        String url = counterDevice.getCallbackUrl() + "/order/incoming";

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `content-type` header
        headers.setContentType(MediaType.APPLICATION_JSON);
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("deviceId", String.valueOf(serverDevice.getId()));

        // create a post object
        OrderPreparationRequest post = OrderPreparationRequest.fromDatabase(order);

        // build the request
        HttpEntity<OrderPreparationRequest> entity = new HttpEntity<>(post, headers);

        // send POST request
        OrderStatusResponse response = restTemplate.postForObject(url, entity, OrderStatusResponse.class);
        if(response == null) {
            return OrderStatus.INTERNAL_ERROR;
        }
        return response.status;
    }
}

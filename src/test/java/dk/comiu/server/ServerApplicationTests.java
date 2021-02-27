package dk.comiu.server;

import dk.comiu.server.db.*;
import dk.comiu.server.payments.DummyPaymentProcessor;
import dk.comiu.server.templates.CounterClientService;
import dk.comiu.server.templates.TableClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class ServerApplicationTests {

    @Autowired
    MockMvc mockMvc;

    MockRestServiceServer tableMockServer;
    MockRestServiceServer counterMockServer;

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    DeviceRepository deviceRepository;
    @Autowired
    SkuRepository skuRepository;
    @Autowired
    ColourRepository colourRepository;
    @Autowired
    SegmentRepository segmentRepository;
    @SpyBean
    DummyPaymentProcessor paymentProcessor;
    @Autowired
    TableClientService tableClientService;
    @Autowired
    CounterClientService counterClientService;

    Device server, counter, table;
    Sku sku1, sku2;
    Segment segment1, segment2, segment3, segment4;
    Colour red, green, blue, yellow;

    @BeforeEach
    public void setUpTest() {
        this.tableMockServer = MockRestServiceServer
                .createServer(tableClientService.getRestTemplate());
        this.counterMockServer = MockRestServiceServer
                .createServer(counterClientService.getRestTemplate());
    }

    @Test
    void contextLoads() {
    }

    @Test
    void successfulFlowTest() throws Exception {
        //given
        //database is set up
        setUpData();
        //when
        //table asks for order
        var request = mockMvc.perform(post("/order/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content("" +
                        "{" +
                        "	\"elements\": [" +
                        "		{ \"sku\": " + sku1.getId() + ", \"quantity\": 1 }, " +
                        "		{ \"sku\": " + sku2.getId() + ", \"quantity\": 3 } " +
                        "	]" +
                        "}" +
                        "")
                .header("deviceId", table.getId()));


        //then
        //table gets confirmation at endpoint
        request.andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{'status': 'OK'}"));
        //PP gets payment request
        Mockito.verify(paymentProcessor).initiatePayment(any(), any());

        //given
        //we have actual order
        Order order = orderRepository.findAll().iterator().next();
        //we have counter and table devices working
        stubResponses(order);
        //when
        //PP responds with acceptance
        paymentProcessor.setPaymentAsAccepted(order);
        //then
        //counter gets order (stub rule)
        //table gets confirmation (stub rule)

        //when
        //counter is ready
        request = mockMvc.perform(post("/order/ready")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + order.getId() + "}")
                .header("deviceId", counter.getId()));
        //then
        //table gets colour (stub rule)
        //counter gets colour
        request.andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{'status': 'OK'}"))
                .andExpect(content().json("{'colour': { 'colour': "+red.getId()+"}}"))
                .andExpect(content().json("{'colour': { 'segment': "+segment1.getId()+"}}"));

        //when
        //counter is done
        request = mockMvc.perform(post("/order/done")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + order.getId() + "}")
                .header("deviceId", counter.getId()));
        //then
        //table gets cancellation (stub rule)
        //counter gets confirmation
        request.andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{'status': 'OK'}"));

        //we also verify all devices got their requests
        tableMockServer.verify();
        counterMockServer.verify();
    }

    void setUpData() {
        //devices exist
        server = new Device();
        server.setType(Device.DeviceType.SERVER);
        server.setCallbackUrl("http://localhost:8080");
        server.setName("Server");
        server = deviceRepository.save(server);
        counter = new Device();
        counter.setType(Device.DeviceType.COUNTER);
        counter.setCallbackUrl("http://localhost:8080"); //TODO
        counter.setName("Counter");
        counter = deviceRepository.save(counter);
        table = new Device();
        table.setType(Device.DeviceType.TABLE);
        table.setCallbackUrl("http://localhost:8080"); //TODO
        table.setName("Table");
        table = deviceRepository.save(table);
        //skus exist
        sku1 = new Sku();
        sku1.setName("Beer 1");
        sku1.setAmount(BigDecimal.valueOf(12.50));
        sku1 = skuRepository.save(sku1);
        sku2 = new Sku();
        sku2.setName("Beer 2");
        sku2.setAmount(BigDecimal.valueOf(17.50));
        sku2 = skuRepository.save(sku2);
        //colours and segments exist
        segment1 = new Segment();
        segment1.setDevice(counter);
        segment1 = segmentRepository.save(segment1);
        segment2 = new Segment();
        segment2.setDevice(counter);
        segment2 = segmentRepository.save(segment2);
        segment3 = new Segment();
        segment3.setDevice(counter);
        segment3 = segmentRepository.save(segment3);
        segment4 = new Segment();
        segment4.setDevice(counter);
        segment4 = segmentRepository.save(segment4);
        red = new Colour();
        red.setName("Red");
        red.setDevice(counter);
        red = colourRepository.save(red);
        green = new Colour();
        green.setName("Green");
        green.setDevice(counter);
        green = colourRepository.save(green);
        blue = new Colour();
        blue.setName("Blue");
        blue.setDevice(counter);
        blue = colourRepository.save(blue);
        yellow = new Colour();
        yellow.setName("Yellow");
        yellow.setDevice(counter);
        yellow = colourRepository.save(yellow);
    }

    void stubResponses(Order order) {
        //Table
        //The order will be accepted
        tableMockServer.expect(once(), requestTo("http://localhost:8080/order/accepted"))
                .andExpect(jsonPath("$.orderId.id").value(order.getId()))
                .andRespond(
                        withSuccess(
                                "{\"orderId\":{\"id\":" + order.getId() + "},\"status\":\"OK\"}",
                                MediaType.APPLICATION_JSON));

        //The order will be ready with some colour
        tableMockServer.expect(once(), requestTo("http://localhost:8080/order/ready"))
                .andExpect(jsonPath("$.orderId.id").value(order.getId()))
                .andExpect(jsonPath("$.colour.colour").value(red.getId()))
                .andExpect(jsonPath("$.colour.segment").value(segment1.getId()))
                .andRespond(
                        withSuccess(
                                "{\"orderId\":{\"id\":" + order.getId() + "},\"status\":\"OK\"}",
                                MediaType.APPLICATION_JSON));
        //The order will be done
        tableMockServer.expect(once(), requestTo("http://localhost:8080/order/done"))
                .andExpect(jsonPath("$.orderId.id").value(order.getId()))
                .andRespond(
                        withSuccess(
                                "{\"orderId\":{\"id\":" + order.getId() + "},\"status\":\"OK\"}",
                                MediaType.APPLICATION_JSON));

        //Counter
        //the order will arrive
        counterMockServer.expect(once(), requestTo("http://localhost:8080/order/incoming"))
                .andExpect(jsonPath("$.id.id").value(order.getId()))
                .andExpect(jsonPath("$.elements").isArray())
                .andExpect(jsonPath("$.elements[0].sku").value(sku1.getId()))
                .andRespond(
                        withSuccess(
                                "{\"orderId\":{\"id\":" + order.getId() + "},\"status\":\"OK\"}",
                                MediaType.APPLICATION_JSON));

    }
}

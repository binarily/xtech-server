package dk.comiu.server;

import dk.comiu.server.db.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SegmentService {
    private final OrderRepository orderRepository;
    private final ColourRepository colourRepository;
    private final SegmentRepository segmentRepository;

    public SegmentService(OrderRepository orderRepository,
                          ColourRepository colourRepository,
                          SegmentRepository segmentRepository) {
        this.orderRepository = orderRepository;
        this.colourRepository = colourRepository;
        this.segmentRepository = segmentRepository;
    }

    public Optional<Order> assignSegment(Order order, Device device) {
        //Check if there are free segments
        Optional<Segment> segmentOpt = segmentRepository.getFirstAvailableSegment(device);
        //If so, mark first free segment to be taken
        if(segmentOpt.isEmpty()){
            return Optional.empty();
        }
        Segment segment = segmentOpt.get();
        //Select first colour that doesn't have a segment
        Colour colour = colourRepository.setFirstAvailableColour(device, segment);

        order.setColour(colour);
        order.setUpdatedBy(device);
        order.setUpdated(LocalDateTime.now());
        orderRepository.save(order);

        return Optional.of(order);
    }

    public Order freeColour(Order order) {
        Colour colour = order.getColour();
        Segment segment = colour.getSegment();
        segment.setTaken(false);
        segmentRepository.save(segment);
        colour.setSegment(null);
        colourRepository.save(colour);

        return order;
    }
}

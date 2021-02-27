package dk.comiu.server.db;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

public interface ColourRepository extends CrudRepository<Colour, Long> {
    Colour findFirstBySegmentIsNullAndDevice(Device device);

    @Transactional
    @Modifying
    default Colour setFirstAvailableColour(Device device, Segment segment) {
        Colour colour = findFirstBySegmentIsNullAndDevice(device);
        colour.setSegment(segment);
        return save(colour);
    }
}
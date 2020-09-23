package COMSETsystem;

/**
 * This class is a copy of the information for a Resource that is useful to the Fleet Manager. It's a copy so that
 * the Fleet Manager cannot change the original Resource's attributes.
 */
public class Resource {
    public final long id;
    public final long expirationTime;
    public final long assignedAgentId;
    public LocationOnRoad pickupLoc;
    public LocationOnRoad dropOffLoc;

    public Resource(long id, long expirationTime, long assignedAgentId, LocationOnRoad pickupLoc,
                    LocationOnRoad dropOffLoc) {
        this.id = id;
        this.expirationTime = expirationTime;
        this.assignedAgentId = assignedAgentId;
        this.pickupLoc = pickupLoc;
        this.dropOffLoc = dropOffLoc;
    }
}

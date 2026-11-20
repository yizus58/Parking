package com.nelumbo.park.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parking {

    private String id;
    private String name;
    private String address;
    private int capacity;
    private Float costPerHour;
    private User owner;

    public boolean isOwnedBy(User user) {
        return owner != null && owner.hasSameIdAs(user);
    }

    /**
     * @param vehiclesInside vehicles currently inside this parking
     * @return true when no more vehicles may enter
     */
    public boolean isFull(long vehiclesInside) {
        return vehiclesInside > 0 && vehiclesInside >= capacity;
    }
}

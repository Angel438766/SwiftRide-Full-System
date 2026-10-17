
package taxicab.model;

import jakarta.persistence.Entity;

@Entity
public class Passenger extends User {

    private String favouriteLocation;

    private boolean active = true;

    public Passenger() {
        super();
    }

    public Passenger(Long id, String name, String email,
                     String password, String phone,
                     String favouriteLocation) {

        super(id, name, email, password, phone);
        this.favouriteLocation = favouriteLocation;
        this.active = true;
    }

    public String getFavouriteLocation() {
        return favouriteLocation;
    }

    public void setFavouriteLocation(String favouriteLocation) {
        this.favouriteLocation = favouriteLocation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

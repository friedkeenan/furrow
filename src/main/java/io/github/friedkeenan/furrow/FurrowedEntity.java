package io.github.friedkeenan.furrow;

import java.util.Optional;

public interface FurrowedEntity {
    public Optional<Furrow> getFurrow();
    public void setFurrow(Optional<Furrow> furrow);

    public default void clearFurrow() {
        this.setFurrow(Optional.empty());
    }
}

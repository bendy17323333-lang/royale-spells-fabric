package dev.royalespells.entity;
import java.util.UUID;
public interface Summoned {
    UUID ownerId();
    void setup(UUID owner,int life,boolean clone);
    boolean isClone();
}

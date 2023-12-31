package mrbubblegum.fastcrystal.utils;

import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class RenderUtil {

    public static List<Entity> renderedEntities = new ArrayList<>();
    public static List<Entity> unrenderedEntities = new ArrayList<>();

    public static boolean isEntityRendered(Entity entity) {
        return renderedEntities.contains(entity) && !unrenderedEntities.contains(entity);
    }
//    public void unrenderEntity(Entity entity) {
//        unrenderedEntities.add(entity);
//    }

//    public static boolean isCrystalRendered(EndCrystalEntity entity) {
//        return getRenderedCrystals().contains(entity);
//    }
//
//    public static List<EndCrystalEntity> getRenderedCrystals() {
//        List<EndCrystalEntity> renderedCrystals = new ArrayList<>();
//        for (Entity renderedEntity : renderedEntities) {
//            if (renderedEntity instanceof EndCrystalEntity crystal)
//                renderedCrystals.add(crystal);
//        }
//        return renderedCrystals;
//    }
}

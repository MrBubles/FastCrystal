package mrbubblegum.fastcrystal.utils;

import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class RenderUtil {

    public static List<Entity> renderedEntities = new ArrayList<>();

    public static boolean isEntityRendered(Entity entity) {
        return renderedEntities.contains(entity);
    }
}

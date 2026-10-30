package dev.tyty.plunderlog;

import net.minecraft.resources.ResourceLocation;

public final class StructureNames {
    private StructureNames() {}

    public static String structure(ResourceLocation id) {
        return title(id.getPath().replace('_', ' '));
    }

    public static String dimension(ResourceLocation id) {
        return switch (id.toString()) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "The Nether";
            case "minecraft:the_end" -> "The End";
            default -> id.toString();
        };
    }

    public static String title(String words) {
        String[] parts = words.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.isEmpty() ? words : out.toString();
    }
}

package fr.euphyllia.tenseimc;

import org.bukkit.Bukkit;

public class Tensei {

    private static TenseiServer server;

    private Tensei() {}

    public static void setup(TenseiServer tenseiServer) {
        if (Tensei.server != null) {
            throw new UnsupportedOperationException("Can't redefine singleton Server");
        }

        Tensei.server = tenseiServer;
        Bukkit.getLogger().info(getVersionMessage());
    }

    private static String getVersionMessage() {
        return "Initialized TenseiMC API version " + Bukkit.getBukkitVersion();
    }

    public static TenseiServer getServer() {
        return server;
    }
}

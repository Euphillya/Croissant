package fr.euphyllia.tenseimc.server;

import net.minecraft.server.ServerAdvancementManager;

public interface PlayerAdvancementHandler {

    void load(ServerAdvancementManager manager);

    void save();
}

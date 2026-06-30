package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;

/**
 * Point d'entrée pour la synchronisation réseau (packets Forge).
 * <p>
 * Les messages de contrôle vidéo, état des écrans et flux NDI seront enregistrés ici.
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {
    }

    /**
     * Enregistre les canaux et handlers réseau. Appelé pendant {@code FMLCommonSetupEvent}.
     */
    public static void register() {
        LumaVisionMod.LOGGER.debug("Réseau LumaVision prêt (protocole v{})", PROTOCOL_VERSION);
        // SimpleChannel channel = NetworkRegistry.newSimpleChannel(...)
    }
}

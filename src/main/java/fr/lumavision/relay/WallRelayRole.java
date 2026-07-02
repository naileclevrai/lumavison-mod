package fr.lumavision.relay;

/**
 * How a client should obtain frames for a merged LED wall in multiplayer.
 */
public enum WallRelayRole {
    /** Single player or relay disabled — capture locally. */
    LOCAL,
    /** This client captures the source and uploads frames to the server. */
    UPLOAD,
    /** This client receives frames relayed by the server. */
    RECEIVE
}

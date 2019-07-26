package com.Smoke.Signals;

import java.util.logging.Level;
import java.util.logging.Logger;

class Logging extends Pubsub {

    //TODO: if 'ready' is false, queue up logs; when 'ready' becomes true, fire the logs into the logging room

    Logging(User yourself) {
        super(yourself, "Error_Reporting", false);
    }

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    void logWarning(String message) {
        LOGGER.log(Level.WARNING, message);
        writeToPubsub("[WARNING] " + message, MessageType.ERROR);
    }

    void logSecurity(String message){
        LOGGER.log(Level.SEVERE, message);
        writeToPubsub("[SEVERE] " + message, MessageType.ERROR);
    }

    void logSevere(String message) {
        LOGGER.log(Level.SEVERE, message);
        writeToPubsub("[SEVERE] " + message, MessageType.ERROR);
    }

    void logConfig(String message) {
        LOGGER.log(Level.CONFIG, message);
    }

    void logINFO(String message) {
        LOGGER.log(Level.INFO, message);
    }

    void logFine(String message) {
        LOGGER.log(Level.FINE, message);
    }

    void logFiner(String message) {
        LOGGER.log(Level.FINER, message);
    }

    static void logFinest(String message) {
        LOGGER.log(Level.FINEST, message);
    }
}

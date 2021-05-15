package com.github.akunzai.log4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendGridTestRunner {
    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger("sync");
        logger.error("error message");
        final Logger asyncLogger = LogManager.getLogger("async");
        asyncLogger.error("error message");
    }
}

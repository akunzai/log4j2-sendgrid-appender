package com.github.akunzai.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SendGridTestRunner {

    private static final Logger LOGGER = LogManager.getLogger(SendGridTestRunner.class);
    private static final Random RANDOM = new Random();
    private static final Level[] LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

    public static void main(String[] args) throws InterruptedException {
        for (var i = 0; i < 512; i++) {
            var level = LEVELS[RANDOM.nextInt(LEVELS.length)];
            LOGGER.log(level, level.toString().toLowerCase(Locale.ENGLISH));
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 10000));
        }
    }
}

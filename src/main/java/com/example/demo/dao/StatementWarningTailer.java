package com.example.demo.dao;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Inspired by
 * <a href="http://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/input/Tailer.html">Apache
 * Commons IO Tailer class</a>.
 */
public class StatementWarningTailer implements Runnable {

    private static final int DEFAULT_DELAY_MILLIS = 1000;

    /**
     * The statement which warnings will be tailed.
     */
    private final Statement statement;

    /**
     * The consumer to notify of warnings when tailing.
     */
    private final Consumer<SQLWarning> warningConsumer;

    /**
     * The amount of time to wait for the warnings to be updated.
     */
    private final long delayMillis;

    private SQLWarning lastWarning;

    /**
     * Creates StatementWarningTailer for the given statement, warning consumer and delay.
     *
     * @param statement       the statement which warnings should be followed
     * @param warningConsumer warning consumer to use
     * @param delayMillis     the delay between checks of the statement for new warnings in milliseconds.
     */
    public StatementWarningTailer(Statement statement, Consumer<SQLWarning> warningConsumer, long delayMillis) {
        this.statement = statement;
        this.warningConsumer = warningConsumer;
        this.delayMillis = delayMillis;
    }

    /**
     * Creates and starts a StatementWarningTailer for the given statement.
     *
     * @param statement       the statement which warnings should be followed
     * @param warningConsumer warning consumer to use
     * @return The new tailer.
     */
    public static StatementWarningTailer create(Statement statement, Consumer<SQLWarning> warningConsumer) {
        final StatementWarningTailer tailer = new StatementWarningTailer(statement, warningConsumer,
                DEFAULT_DELAY_MILLIS);
        final Thread thread = new Thread(tailer);
        thread.setDaemon(true);
        thread.start();
        return tailer;
    }

    /**
     * Inspired by "Implementing cancelable tasks" section of
     * <a href="https://www.ibm.com/developerworks/library/j-jtp05236/">Dealing with InterruptedException</a>
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    readWarnings();
                } catch (SQLException e) {
                    // dont' do nothing in this demo implementation
                }
                TimeUnit.MILLISECONDS.sleep(delayMillis);
            }
        } catch (InterruptedException consumed) {
            /* Allow thread to exit */
            beforeThreadExit();
        }
    }

    private void beforeThreadExit() {
        try {
            readWarnings();
        } catch (SQLException e) {
            // dont' do nothing in this demo implementation
        }
    }

    private void readWarnings() throws SQLException {
        SQLWarning warning = lastWarning != null ? lastWarning.getNextWarning() : statement.getWarnings();
        while (warning != null) {
            warningConsumer.accept(warning);
            lastWarning = warning;
            warning = warning.getNextWarning();
        }
    }
}
package com.example.demo.dao;

import org.hibernate.Session;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.*;
import java.util.function.Consumer;

@Repository
public class SampleDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Optional task executor in case we would like to start helper threads in a centralized manner.
     */
    private TaskExecutor taskExecutor;

    /**
     * Executes a database function that is a hypothetical long-running task.
     *
     * @param subTaskCount    number of subtasks (units of work), each subtask adds a delay to the whole task running
     *                        time (for demonstration purpose mainly)
     * @param warningConsumer SQL warning received from function consumer
     * @return database function result
     */
    @Transactional
    public boolean longRunningTask(int subTaskCount, Consumer<SQLWarning> warningConsumer) {
        final Boolean[] result = new Boolean[1];

        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> result[0] = executeObservableStatement(connection, subTaskCount, warningConsumer));
        return result[0];
    }

    private boolean executeObservableStatement(Connection connection, int subTaskCount,
                                               Consumer<SQLWarning> warningConsumer) throws SQLException {
        PreparedStatement st = connection.prepareStatement("select long_running_task(?)");
        st.setInt(1, subTaskCount);
        // Start the tailer that will follow statement warnings
        StatementWarningTailer tailer = new StatementWarningTailer(st, warningConsumer, 500);
        Thread thread = new Thread(tailer);
        if (taskExecutor != null) {
            taskExecutor.execute(thread);
        } else {
            thread.start();
        }
        // Execute the statement
        ResultSet rs = st.executeQuery();
        // Wait for tailer thread stop
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Return statement result
        if (rs.next()) {
            return rs.getBoolean(1);
        } else {
            return false;
        }
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }
}
package com.streamunlimited.streamsdkdemo.helper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 */
public class CurrentTaskExecutor {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture _taskHandle;

    public void startExecution(int milliSecInterval) {

        final Runnable taskContainer = () -> {
            if (_currTask != null) {
                // make the task thread save
                final Runnable task = _currTask;
                task.run();

                // remove the current task, so that it isn't executed twice
                _currTask = null;
            }
        };

        _taskHandle = scheduler.scheduleAtFixedRate(taskContainer, 0, milliSecInterval, MILLISECONDS);
    }

    public void stopExecution() {
        _taskHandle.cancel(true);
    }

    private Runnable _currTask = null;
    public void setCurrTask(Runnable value) {
        _currTask = value;
    }

}

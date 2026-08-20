package com.github.athanh.royAutoRestart.scheduler;

/**
 * Interface representing a scheduled cancelable task.
 */
@FunctionalInterface
public interface TaskWrapper {
    /**
     * Cancel the task execution.
     */
    void cancel();
}

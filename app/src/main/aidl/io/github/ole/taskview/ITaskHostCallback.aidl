package io.github.ole.taskview;

interface ITaskHostCallback {

    /**
     * Set back interceptable in task
     * @param enabled send back event in task to host or not
     */
    oneway void setBackInterceptable(boolean enabled);
}

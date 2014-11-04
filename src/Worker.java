/**
 * Created by megge on 04.11.14.
 */

public class Worker extends Thread {

    private String workerId;
    private Runnable task;
    private ThreadPool threadpool;

    public Worker(String id, ThreadPool pool) {
        this.workerId = id;
        this.threadpool = pool;
        start();
    }

    public void setTask(Runnable task) {
        this.task = task;
        synchronized (this) {
            notify();
        }
    }

    public void run() {
        try {
            while (!threadpool.isStopped()) {
                synchronized (this) {
                    if (task != null) {
                        try {
                            // Запускаем задачу
                            task.run();
                        }
                        catch (Exception exception) {
                        }
                        // Возвращает себя в пул нитей
                        threadpool.putWorker(this);
                    }
                    wait();
                }
            }
        }
        catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }
}

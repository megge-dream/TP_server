/**
 * Created by megge on 02.11.14.
 */
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class ThreadPool extends Thread {

    private static final int DEFAULT_NUMBER_WORKERS = 2;

    private LinkedList workerPool = new LinkedList();
    private LinkedList taskList = new LinkedList();
    private boolean stopped = false;
    private int countWorkers;

    public ThreadPool(){
        this(DEFAULT_NUMBER_WORKERS);
    }

    public ThreadPool(int countWorkers) {
        this.countWorkers = countWorkers;
        init();
    }

    private void init(){
        for (int i = 0; i < countWorkers; i++)
            workerPool.add(new Worker("" + i, this));
        start();
    }

    public void run() {
        try {
            while (!stopped) {
                if (taskList.isEmpty()) {
                    synchronized (taskList) {
                        // Если очередь пустая, подождать, пока будет добавлена новая задача
                        taskList.wait();
                    }
                }
                else if (workerPool.isEmpty()) {
                    synchronized (workerPool) {
                        // Если нет рабочих потоков, подождать, пока не появится
                        workerPool.wait();
                    }
                } else {
                    // Запускаем следующую задачу из списка задач
                    try {
                        Worker newWorker = getWorker();
                        newWorker.setTask((Runnable) taskList.removeLast());
                    } catch (NoSuchElementException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void addTask(Runnable task) {
        taskList.addFirst(task);
        synchronized (taskList) {
            // Если добавлена новая задача, уведомляем
            taskList.notify();
        }
    }

    public void putWorker(Worker worker) {
        workerPool.addFirst(worker);
        // Когда нет свободных потоков в пуле, то блокируем пул, до тех пор пока они не появятся
        synchronized (workerPool) {
            workerPool.notify();
        }
    }

    private Worker getWorker() {
        Worker w = (Worker) workerPool.getLast();
        workerPool.remove();
        return w;
    }

    public boolean isStopped() {
        return stopped;
    }
}
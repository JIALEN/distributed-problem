package com.alen.distributed.problem.lock.zk.javaapi;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.util.concurrent.CountDownLatch;

/**
 * 当节点发生变化时，通过watcher机制，可以让客户端得到通知，
 * watcher需要实现org.apache.ZooKeeper.Watcher接口。
 * @author alen
 * @create 2018-11-15 16:07
 **/
public class LockWatcher implements Watcher {
    private CountDownLatch latch;
    public LockWatcher(CountDownLatch latch) {
        this.latch = latch;
    }
    @Override
    public void process(WatchedEvent watchedEvent) {
        if(watchedEvent.getType()== Event.EventType.NodeDeleted){
            latch.countDown();
        }
    }
}

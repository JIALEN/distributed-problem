package com.alen.distributed.problem.lock.zk.cutator.reentrant;

import com.alen.distributed.problem.lock.zk.javaapi.DistributeLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 基于zk用curator实现可重入读写锁
 *
 * @author alen
 * @create 2018-11-15 23:41
 **/
public class ZKSharedReentrantReadWriteLock {
    private static final Logger log = LoggerFactory.getLogger(ZKSharedReentrantReadWriteLock.class);
    private final InterProcessReadWriteLock lock;
    private final InterProcessMutex readLock;
    private final InterProcessMutex writeLock;
    private static final String lockPAth = "/zk-SharedReentrantReadWrite-lock";
    private final FakeLimitedResource resource;
    private String clientName;
    private static CuratorFramework curatorFramework;
    private static final int QTY = 5;
    private static final int REPETITIONS = QTY * 10;
    //zookeeper集群地址
    public static final String ZOOKEEPERSTRING = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";

    static {
        CuratorFramework client = CuratorFrameworkFactory.newClient(ZOOKEEPERSTRING, new ExponentialBackoffRetry(1000, 3));
        client.start();
        curatorFramework = client;
    }

    public ZKSharedReentrantReadWriteLock(CuratorFramework client, String lockPath, FakeLimitedResource resource, String clientName) {
        this.resource = resource;
        this.clientName = clientName;
        lock = new InterProcessReadWriteLock(client, lockPath);
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }


    public void doWork(long time, TimeUnit unit) throws Exception {
        // 注意只能先得到写锁再得到读锁，不能反过来！！！
        if (!writeLock.acquire(time, unit)) {
            throw new IllegalStateException(clientName + " 不能得到写锁");
        }
        System.out.println(clientName + " 已得到写锁");
        if (!readLock.acquire(time, unit)) {
            throw new IllegalStateException(clientName + " 不能得到读锁");
        }
        System.out.println(clientName + " 已得到读锁");
        try {
            resource.use(); // 使用资源
            Thread.sleep(1000 * 1);
        } finally {
            System.out.println(clientName + " 释放读写锁");
            readLock.release();
            writeLock.release();
        }
    }



    public static void main(String[] args) throws Exception {
        final FakeLimitedResource resource = new FakeLimitedResource();
        ExecutorService service = Executors.newFixedThreadPool(QTY);
        try {
            for (int i = 0; i < QTY; ++i) {
                final int index = i;
                Callable<Void> task = new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            final ZKSharedReentrantReadWriteLock example = new ZKSharedReentrantReadWriteLock(curatorFramework, lockPAth, resource, "Client " + index);
                            for (int j = 0; j < REPETITIONS; ++j) {
                                example.doWork(10, TimeUnit.SECONDS);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                service.submit(task);
            }
            service.shutdown();
            service.awaitTermination(10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw e;
        }
    }

}

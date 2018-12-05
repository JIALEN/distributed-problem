package com.alen.distributed.problem.lock.zk.cutator.reentrant;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 不可重入锁
 * @author alen
 * @create 2018-11-15 22:14
 **/
public class ZKCuratorNOReentrantLock {
    private static final Logger log = LoggerFactory.getLogger(ZKCuratorNOReentrantLock.class);
    private InterProcessSemaphoreMutex lock;//不可重入锁
    private static String lockPAth = "/noreentrantlock/shareLock";
    private final FakeLimitedResource resource;
    private static CuratorFramework curatorFramework;
    private String clientName;
    //zookeeper集群地址
    public static final String ZOOKEEPERSTRING = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
    private static final int QTY = 5;
    private static final int REPETITIONS = QTY * 10;

    static {
        CuratorFramework client = CuratorFrameworkFactory.newClient(ZOOKEEPERSTRING, new ExponentialBackoffRetry(1000, 3));
        client.start();
        curatorFramework = client;
    }


    public ZKCuratorNOReentrantLock(CuratorFramework client, String lockPath, FakeLimitedResource resource, String clientName) {
        this.resource = resource;
        this.clientName = clientName;
        this.lock = new InterProcessSemaphoreMutex(client, lockPath);
    }

   //不可重入锁只能获得一次
    public void doWork(long time, TimeUnit unit) throws Exception {
        if (!lock.acquire(time, unit)) {
            throw new IllegalStateException(clientName + " 不能得到互斥锁");
        }
        log.info(clientName + " 已获取到互斥锁");
        if (!lock.acquire(time, unit)) {
            throw new IllegalStateException(clientName + " 不能得到互斥锁");
        }
        log.info(clientName + " 再次获取到互斥锁");
        try {
            resource.use(); // 使用资源
            Thread.sleep(1000 * 1);
        } finally {
            log.info(clientName + " 释放互斥锁");
            lock.release(); // 总是在finally中释放
            lock.release(); // 获取锁几次 释放锁也要几次
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
                            final ZKCuratorNOReentrantLock example = new ZKCuratorNOReentrantLock(curatorFramework, lockPAth, resource, "Client " + index);
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

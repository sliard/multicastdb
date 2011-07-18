/**
 * Copyright 2011 liard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.netliard.activemq.transport.discovery.database;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DiscoveryAgent that use a database to get the liste of broker.
 * 
 * I have try to use multicast discovery but in many cloud offer like EC2 multicast UDP didn't work.
 * 
 * @author Samuel Liard
 * 
 */
public class DataBaseDiscoveryAgent implements DiscoveryAgent, Runnable {

    /**
     * logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataBaseDiscoveryAgent.class);

    /**
     * Cache list of broker.
     */
    private Map<String, RemoteBrokerData> brokersByService = new ConcurrentHashMap<String, RemoteBrokerData>();

    /**
     * Name of spring Data Source.
     */
    private String dataSource;

    /**
     * Define if agent is started or not.
     */
    private AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Background Thread to scan if new brokers arrived.
     */
    private Thread runner;

    /**
     * MQ listner to add/remove broker.
     */
    private DiscoveryListener discoveryListener;

    /**
     * Local service name (URI).
     */
    private String selfService;

    /**
     * Asynchrone task manager.
     */
    private ExecutorService executor = null;

    /**
     * Time in ms between 2 new broker scan.
     */
    private int scanInterval = 10000;

    /**
     * Time max of last hearbeat broker in ms.
     */
    private int maxDelay = 25000;

    /**
     * Server hostName use for distant client.
     */
    private String hostName;

    /**
     * JDBC link with data base.
     */
    private JDBCAdapter adapter;

    /**
     * Define Broker identifier.
     * 
     * @return broker identifier
     */
    private String getLocalBrokerKey() {
        return "lb-" + getHostName();
    }

    /**
     * Get Data Base link.
     * 
     * @return Data Base
     */
    private JDBCAdapter getAdapter() {
        if (adapter == null) {
            adapter = new JDBCAdapter(dataSource);
        }
        return adapter;
    }

    private String getHostName() {
        if (hostName == null) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                // hostName = addr.getHostAddress();
                hostName = addr.getHostName();
            } catch (UnknownHostException e) {
                LOG.warn("Unable to get hostname : " + e.getMessage());
                hostName = "Unknown";
            }
        }
        return hostName;
    }

    @Override
    public void start() throws Exception {

        LOG.trace("Start DataBaseDiscoveryAgent");

        if (started.compareAndSet(false, true)) {

            getAdapter().initDB();
            doAdvertizeSelf();
            scanBroker();

            runner = new Thread(this);
            runner.setName(this.toString() + ":" + runner.getName());
            runner.setDaemon(true);
            runner.start();
        }

    }

    @Override
    public void stop() throws Exception {

        LOG.trace("Stop DataBaseDiscoveryAgent");

        if (started.compareAndSet(true, false)) {
            if (runner != null) {
                runner.interrupt();
            }
        }

    }

    @Override
    public void run() {

        while (started.get()) {

            try {
                doAdvertizeSelf();
                scanBroker();
                Thread.sleep(scanInterval);
            } catch (InterruptedException e) {
                LOG.warn("Thread interrupted : " + e.getMessage());
            }
        }
    }

    /**
     * Manage local broker.
     */
    private synchronized void doAdvertizeSelf() {
        if (selfService != null) {

            RemoteBrokerData selfData = brokersByService.get(getLocalBrokerKey());

            if (selfData == null) {
                selfData = new RemoteBrokerData(getLocalBrokerKey(), selfService);
                selfData.setLocal(true);
                fireServiceAddEvent(selfData);
                LOG.trace("Send local broker : {}", selfData);

                String serviceWithIp = selfService;
                int localIndex = serviceWithIp.indexOf("localhost");

                if (localIndex != -1) {
                    int portIndex = serviceWithIp.indexOf(":", localIndex);
                    serviceWithIp = serviceWithIp.substring(0, localIndex) + getHostName() + serviceWithIp.substring(portIndex);
                }

                RemoteBrokerData selfIpData = new RemoteBrokerData(getLocalBrokerKey(), serviceWithIp);
                getAdapter().addBroker(selfIpData);
                getAdapter().updateBroker(selfIpData);

                brokersByService.put(getLocalBrokerKey(), selfIpData);
                LOG.trace("Add self broker : {}", selfIpData);
            } else {
                selfData.updateHeartBeat();
                getAdapter().updateBroker(selfData);
            }

        }
    }

    private void scanBroker() {
        Collection<RemoteBrokerData> allBroker = getAdapter().getAllBroker(maxDelay);
        RemoteBrokerData[] arrayOfBroker = brokersByService.values().toArray(new RemoteBrokerData[0]);

        for (RemoteBrokerData broker : arrayOfBroker) {
            if (!allBroker.contains(broker) && !broker.isLocal()) {
                fireServiceRemovedEvent(broker);
                brokersByService.remove(broker.getBrokerName());
                LOG.trace("Delete broker : {}", broker);
            }
        }

        for (RemoteBrokerData broker : allBroker) {
            RemoteBrokerData data = brokersByService.get(broker.getBrokerName());
            if (data == null) {
                brokersByService.put(broker.getBrokerName(), broker);
                fireServiceAddEvent(broker);
                LOG.trace("Add broker : {}", broker);
            }
        }
    }

    private void fireServiceRemovedEvent(RemoteBrokerData data) {
        if (discoveryListener != null && started.get()) {
            final DiscoveryEvent event = new DiscoveryEvent(data.getService());
            event.setBrokerName(data.getBrokerName());

            // Have the listener process the event async so that
            // he does not block this thread since we are doing time sensitive
            // processing of events.
            getExecutor().execute(new Runnable() {
                public void run() {
                    DiscoveryListener discoveryListener = DataBaseDiscoveryAgent.this.discoveryListener;
                    LOG.trace("onServiceRemove send to discovery {} : {}", discoveryListener, event.getServiceName());
                    if (discoveryListener != null) {
                        discoveryListener.onServiceRemove(event);
                    }
                }
            });
        }
    }

    private void fireServiceAddEvent(RemoteBrokerData data) {
        if (discoveryListener != null && started.get()) {

            String service = data.getService();
            if (service.contains(getHostName())) {
                service = service.replaceAll(getHostName(), "localhost");
            }

            final DiscoveryEvent event = new DiscoveryEvent(service);
            event.setBrokerName(data.getBrokerName());

            // Have the listener process the event async so that
            // he does not block this thread since we are doing time sensitive
            // processing of events.
            getExecutor().execute(new Runnable() {
                public void run() {
                    DiscoveryListener discoveryListener = DataBaseDiscoveryAgent.this.discoveryListener;
                    LOG.trace("onServiceAdd send to discovery {} : {}", discoveryListener, event.getServiceName());
                    if (discoveryListener != null) {
                        discoveryListener.onServiceAdd(event);
                    }
                }
            });
        }
    }

    private ExecutorService getExecutor() {
        if (executor == null) {
            final String threadName = "Notifier-" + this.toString();

            executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                public Thread newThread(Runnable runable) {
                    Thread t = new Thread(runable, threadName);
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return executor;
    }

    @Override
    public void setDiscoveryListener(DiscoveryListener listener) {
        this.discoveryListener = listener;
        LOG.trace("setDiscoveryListener {}", listener);
    }

    @Override
    public void registerService(String service) throws IOException {
        this.selfService = service;
        LOG.trace("register : {}", service);
    }

    @Override
    public void serviceFailed(DiscoveryEvent event) throws IOException {

        LOG.trace("serviceFailed : {}", event.getBrokerName());

        RemoteBrokerData data = new RemoteBrokerData(event.getBrokerName(), event.getServiceName());
        fireServiceRemovedEvent(data);
        brokersByService.remove(event.getBrokerName());

        // TODO manage add/remove cycle
    }

    public int getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

}

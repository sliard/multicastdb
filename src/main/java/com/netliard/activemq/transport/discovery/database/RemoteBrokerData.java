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

/**
 * POJO to describe Broker.
 * 
 * @author Samuel Liard
 *
 */
public class RemoteBrokerData {

	/**
	 * Broker descriptor.
	 */
	private String brokerName;
	
	/**
	 * Broker service URI.
	 */
	private String service;
	
	/**
	 * Last communitaion date of broker in ms.
	 */
	private long lastHeartBeat;
	
	/**
	 * Is my local broker.
	 */
	private boolean local;

	public RemoteBrokerData(String brokerName, String service) {
		this.brokerName = brokerName;
		this.service = service;
		this.lastHeartBeat = System.currentTimeMillis();
		local = false;
	}

	/**
	 * Set lastHeartBeat to now.
	 */
	public synchronized void updateHeartBeat() {
		lastHeartBeat = System.currentTimeMillis();
	}

	public synchronized long getLastHeartBeat() {
		return lastHeartBeat;
	}

	public String getBrokerName() {
		return brokerName;
	}

	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
			this.service = service;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RemoteBrokerData)) {
			return false;
		}
		String serviceObj = ((RemoteBrokerData) obj).getService();
		String brokerNameObj  = ((RemoteBrokerData) obj).getBrokerName();

		return (!(serviceObj == null || !serviceObj.equals(service)) && (!(brokerNameObj == null || !brokerNameObj.equals(brokerName))));
	}

	@Override
	public String toString() {
		return "RemoteBrokerData[service=" + service + ",name=" + brokerName + "]";
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

}

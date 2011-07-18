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
import java.net.URI;
import java.util.Map;

import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryAgentFactory;
import org.apache.activemq.transport.discovery.database.DataBaseDiscoveryAgent;
import org.apache.activemq.util.IOExceptionSupport;
import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.URISupport;

/**
 * Factory to create DataBaseDiscoveryAgent.
 * 
 * @author Samuel Liard
 *
 */
public class DataBaseDiscoveryAgentFactory extends DiscoveryAgentFactory {

	/**
	 * create DataBaseDiscoveryAgent.
	 * 
	 * @param uri service uri
	 * @return the DiscoveryAgent
	 */
	protected DiscoveryAgent doCreateDiscoveryAgent(URI uri) throws IOException {
		try {

			DataBaseDiscoveryAgent dba = new DataBaseDiscoveryAgent();

			// allow MDA's params to be set via query arguments
			// (e.g., multicastdb://default?dataSource=myDataSource
			Map<String,String> options = URISupport.parseParameters(uri);
			IntrospectionSupport.setProperties(dba, options);

			return dba;

		} catch (Throwable e) {
			throw IOExceptionSupport.create("Could not create discovery agent: " + uri, e);
		}
	}

}

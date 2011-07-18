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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netliard.SpringHook;

/**
 * First implementation of JDBC Adapter.
 * Work with MySQL and derby, but JDBCAdapter nee to be an interface with one implementation by database (like store)
 * 
 * @author Samuel Liard
 *
 */
public class JDBCAdapter {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JDBCAdapter.class);

    /**
     * Data base access.
     */
    DataSource ds;

    public JDBCAdapter(String beanName) {
        ds = (DataSource) SpringHook.getBean(beanName);
    }

    /**
     * Create Broker table.
     */
    public void initDB() {
        Statement stmt = null;
        Connection conn = null;

        try {
            conn = ds.getConnection();
            stmt = conn.createStatement();

            String createString = "create table Broker " + "(service VARCHAR(100) NOT NULL, name VARCHAR(100), lastHeartBeat BIGINT, "
                    + "primary key(service))";

            stmt.executeUpdate(createString);

        } catch (Exception e) {
            LOG.trace("Create table error : " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                LOG.error("Close Statement error", e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                LOG.error("Close Connection error", e);
            }
        }

    }

    /**
     * Add a new broker.
     * 
     * @param data Broker data
     */
    public void addBroker(RemoteBrokerData data) {

        Statement stmt = null;
        Connection conn = null;

        try {
            conn = ds.getConnection();
            stmt = conn.createStatement();

            String createString = "INSERT INTO Broker (service,name,lastHeartBeat) VALUES('" + data.getService() + "','" + data.getBrokerName() + "',"
                    + data.getLastHeartBeat() + ")";

            stmt.executeUpdate(createString);

        } catch (Exception e) {
            LOG.trace("Add broker error : " + e.getMessage());

        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                LOG.error("Close Statement error", e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                LOG.error("Close Connection error", e);
            }
        }

    }

    /**
     * Update lastHeartBeat time for a broker.
     *  
     * @param data Broker data
     */
    public void updateBroker(RemoteBrokerData data) {

        Statement stmt = null;
        Connection conn = null;

        try {
            conn = ds.getConnection();
            stmt = conn.createStatement();

            String createString = "UPDATE Broker SET lastHeartBeat=" + data.getLastHeartBeat() + " WHERE service='" + data.getService() + "'";

            stmt.executeUpdate(createString);

        } catch (Exception e) {
            LOG.trace("Update broker error : " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                LOG.error("Close Statement error", e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                LOG.error("Close Connection error", e);
            }
        }

    }

    /**
     * Get all broker list.
     * 
     * @param maxDelay minimum time in ms of last broker heartBeat
     * @return list of broker data
     */
    public Collection<RemoteBrokerData> getAllBroker(long maxDelay) {

        Collection<RemoteBrokerData> result = new ArrayList<RemoteBrokerData>();

        Statement stmt = null;
        Connection conn = null;
        ResultSet results = null;

        try {
            conn = ds.getConnection();
            stmt = conn.createStatement();
            long dateMin = System.currentTimeMillis() - maxDelay;
            results = stmt.executeQuery("SELECT service,name FROM Broker WHERE lastHeartBeat > " + dateMin);

            while (results.next()) {
                String service = results.getString(1);
                String name = results.getString(2);

                RemoteBrokerData data = new RemoteBrokerData(name, service);
                result.add(data);
            }

        } catch (SQLException e) {
            LOG.warn("SQL error to get all broker : " + e.getMessage());
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
            } catch (Exception e) {
                LOG.error("Close ResultSet error", e);
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                LOG.error("Close Statement error", e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                LOG.error("Close Connection error", e);
            }
        }

        return result;
    }
}

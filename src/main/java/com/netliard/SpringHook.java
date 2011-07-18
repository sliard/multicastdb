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
package com.netliard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.netliard.activemq.transport.discovery.database.JDBCAdapter;

/**
 * Singleton to get Sring Application Context outside a Bean.
 * 
 * @author Samuel Liard
 *
 */
public class SpringHook implements ApplicationContextAware {
    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JDBCAdapter.class);

    
    private static ApplicationContext context;

	  /**
	   * Spring context injection.
	   * @param context reference to the ApplicationContext.
	   */
	  public void setApplicationContext(ApplicationContext myContext) throws BeansException {
	      context = myContext;
	  }

	  /**
	   * Get a bean
	   * @param beanName name of the bean to get.
	   * @return Object reference to the named bean.
	   * @throws IllegalArgumentException if spring context is not set
	   * @throws NoSuchBeanDefinitionException if there is no bean definition with the specified name
       * @throws BeansException if the bean could not be obtained
	   */
	  public static Object getBean(String beanName) {
	      if(context == null) {
	          LOG.error("Can't get Spring context");
              LOG.error("****************************************");
              LOG.error("** Please insert this bean in your Spring configuration : ");
              LOG.error("** <bean id=\"sContext\" class=\"com.netliard.SpringHook\"/> ");
              LOG.error("****************************************");
              throw new IllegalArgumentException("Can't get com.netliard.SpringHook bean");
	      }
	      
	    return context.getBean(beanName);
	  }

}

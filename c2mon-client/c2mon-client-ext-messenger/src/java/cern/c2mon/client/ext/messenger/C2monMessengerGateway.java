/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.client.ext.messenger;

import cern.c2mon.client.core.C2monServiceGateway;
import org.springframework.context.ApplicationContext;

public class C2monMessengerGateway {

  private static BroadcastMessageService broadcastMessageService = null;
  
  private static ApplicationContext context;

  private C2monMessengerGateway() {}

  /**
   * Initializes the C2MON BroadcastMessageService.
   */
  public static synchronized void initialize() {
    if (C2monServiceGateway.getApplicationContext() == null) {
      C2monServiceGateway.startC2monClientSynchronous();
    }
    
    if (context == null) {
      context = C2monServiceGateway.getApplicationContext();
      broadcastMessageService = context.getBean(BroadcastMessageService.class);
    }
  }

  public static BroadcastMessageService getBroadcastMessageService() {
    if (context == null) {
      initialize();
    }
    
    return broadcastMessageService;
  }
}

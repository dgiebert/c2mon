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
package cern.c2mon.server.daq.out;


/**
 * Specification of the beans responsible for sending messages to the DAQ
 * layer and receiving a response.
 *
 * <p>This interface should remain provider-independent.
 *
 * @author Mark Brightwell
 *
 */
public interface JmsProcessOut {

  /**
   * Sends a text message to the DAQ with the text as content, using
   * the JMS queue provided (TODO still Topic in implementation -
   * should be changed at some point??).
   *
   * @param text the content of the message
   * @param jmsListenerQueue the JMS queue to send the message to (as String)
   * @param timeout the timeout while waiting for a response from the DAQ
   * @return the text of the response message
   */
  String sendTextMessage(String text, String jmsListenerQueue, long timeout);

}

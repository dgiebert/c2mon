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
package cern.c2mon.pmanager.mock;

import cern.c2mon.pmanager.IFallback;
import cern.c2mon.pmanager.fallback.exception.DataFallbackException;

/**
 * This class is a fake implementation of the IFallback interface. It is only used for testing, without having
 * to reference any of the real implementations which are stored in different projects.
 * @author mruizgar
 *
 */
public class FallbackImpl implements IFallback {
    
   
    /** String constant identifying one FallbackImpl object*/     
    private static final String STR_LINE = "99999\tUNKNOWN\t0\t2009-01-29\t11:56:50.358\ttrue\tBoolean\tmruizgar\tpcst999\t2009-01-30\t15:37:41.1042009\t0\tOk"; 
    
    /** Contains the string representation of a FallbackImpl object */
    private String objectData;
    
    /** This will be used to simulate a syntax error in the fallback file*/
    public static final String ERROR = "Error"; 
   
  
    /**
     * Returns the id representing this object
     * @return String The object's id
     */
    public final String getId() {
        return "99999";
    }
    
    
    /**
     * @param objectData the objectData to set
     */
    public final void setObjectData(final String objectData) {
        this.objectData = objectData;
    }

    /**
     * Constructs a FallbackImpl object from the string received as a parameter
     * @param line The string representing the fallbackImpl object
     * @return A FallbackImpl object 
     * @throws DataFallbackException An exception is thrown in case that something happens during the string
     * transformation to the object
     */
    public final IFallback getObject(final String line) throws DataFallbackException {
        FallbackImpl fImpl = new FallbackImpl();
        fImpl.setObjectData(line);
        return fImpl;
        
    }
    
    /**
     * Converts a FallbackImpl object into a string representation
     * @return The object's string representation
     */
    public final String toString() {       
        if (this.objectData == null) {
            this.setObjectData(STR_LINE);
        }
        return this.objectData;
                
    }

}

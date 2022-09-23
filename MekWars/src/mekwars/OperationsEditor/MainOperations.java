/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

/**
 * @author Torren (Jason Tighe)
 * 
 * Seperate app to create operations from. This files will then have
 * to be loaded to the server.
 * 
 */
package OperationsEditor;


import OperationsEditor.dialog.OperationsDialog;

public class MainOperations{

    //Main-Method
    /**
     * main call. Clean and simple 
     */
    public static void main(String[] args) {
        try {
        	new OperationsDialog(null);
        } catch (Exception e) {
        	System.err.println(e);
        }
    }
    
    public static void main(Object o) {
        try {
        	new OperationsDialog(o);
        } catch (Exception e) {
        	System.err.println(e);
        }
    }
}
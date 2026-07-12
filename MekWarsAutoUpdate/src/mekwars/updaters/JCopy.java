/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Torren (torren@users.sourceforge.net)
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
 *
 * @author Torren (Jason Tighe) 02.16.06
 *
 */
package mekwars.updaters;

class JCopy {

    JCopy() {
    }

    public void copyFile(java.io.File in, java.io.File out) {

        System.err
              .println("Copying " + in.toString() + " to " + out.toString());
        try (java.io.FileInputStream fis = new java.io.FileInputStream(in);
              java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
            byte[] buf = new byte[1024];
            int i;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            in.delete();
        }
    }
}

/*
 * $Id: VoidFunction.java,v 1.1 2006/05/22 19:42:34 torren Exp $
 *
 * Integerated autoupdater into MekWarsAutoupdate --Torren 
 * 
 * $Log: VoidFunction.java,v $
 * Revision 1.1  2006/05/22 19:42:34  torren
 * + Added new MekWarsAutoUpdate.jar run in stand alone this can be used to update
 * 	the Client or create a manifest.
 *
 * Revision 1.2  2003/09/06 21:49:23  wurp
 * Migrated stuff from ARMI into here
 *
 * Revision 1.1  2003/08/18 17:12:42  gergiskhan
 * Refactoring.  Changed package structure.
 *
 * Revision 1.1  2003/08/08 02:17:21  gergiskhan
 * no message
 *
 * Revision 1.1.1.1  2001/06/26 00:55:49  wurp
 * importing initial rev of source
 *
 * Revision 1.4  2001/06/20 02:58:53  wurp
 * Updated to log4j.  Added default ctor to CharacterInfo
 *
 * Revision 1.3  2001/01/28 07:52:19  wurp
 * Removed <dollar> from Id and Log in log comments.
 * Added several new commands to AdminApp
 * Unfortunately, several other changes that I have lost track of.  Try diffing this
 * version with the previous one.
 *
 * Revision 1.2  2000/12/16 22:07:33  wurp
 * Added Id and Log to almost all of the files that didn't have it.  It's
 * possible that the script screwed something up.  I did a commit and an update
 * right before I ran the script, so if a file is screwed up you should be able
 * to fix it by just going to the version before this one.
 *
 */

// Copyright(c) 1996,1997 ObjectSpace, Inc.
// Portions Copyright(c) 1995, 1996 Hewlett-Packard Company.

package updaters.utils;

import java.io.Serializable;



public interface VoidFunction extends Serializable
{

	/**
	 * Return the result of executing
	 * @return The result of processing the input parameters.
	 */
	Object execute();
}

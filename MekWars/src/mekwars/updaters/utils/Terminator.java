/*
 * $Id: Terminator.java,v 1.2 2006/09/04 17:03:15 urgru Exp $
 * 
 *  * Integerated autoupdater into MekWarsAutoupdate --Torren 
 * 
 */

package updaters.utils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


public class Terminator
{
	protected Terminator()
	{
		//create a new thread to kill this app when a file called 'killme'
		//or 'killme.txt' is created in the directory from which the app
		//was started
		Thread killer = new Thread( "killer" )
		{
			@Override
			public void run()
			{
				File killer1 = new File( "killme" );
				File killer2 = new File( "killme.txt" );
				while ( true )
				{
					if ( killer1.exists() || killer2.exists() )
					{
						String killString =
						"killme file found; deleting and " +
						"calling terminator.exit(-13)";
						System.err.println( killString );
						System.err.println( killString );

						killer1.delete();
						killer2.delete();
						Terminator.instance().exit( -13 );
					}

					try
					{
						Thread.sleep( KILL_CHECK_INTERVAL );
					}
					catch ( Exception e )
					{
					}
				}
			}
		};

		killer.setDaemon( true );
		killer.start();
	}

	public static Terminator instance()
	{
		if ( instance_ == null )
		{
			instance_ = new Terminator();
		}

		return instance_;
	}

	public void runOnExit( VoidFunction vf )
	{
		jobsToRunOnExit_.add( 0, vf );
	}

	public void exit( int status )
	{
		runExitJobs();
		System.gc();
		System.runFinalization();
		//System.exit( status );
	}

	@Override
	public void finalize()
	{
		runExitJobs();
	}

	public void runExitJobs()
	{
		for ( VoidFunction vf : jobsToRunOnExit_)
		{
			vf.execute();
		}
	}

	protected List<VoidFunction> jobsToRunOnExit_ = new LinkedList<VoidFunction>();

	protected static Terminator instance_;

	//check every 5 seconds for a killme or killme.txt file
	public static final int KILL_CHECK_INTERVAL = 5000;

	//com.navtools.networking.armi.networking.test code follows
	public static void main( String[] args )
	{
	}
}

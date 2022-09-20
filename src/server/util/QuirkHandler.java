package server.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.StringJoiner;

import common.util.MWLogger;
import megamek.common.QuirksHandler;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import server.campaign.CampaignMain;
import server.campaign.SUnit;

//@Salient this is a wrapper class for MM's QuirksHandler
public class QuirkHandler 
{
	private static QuirkHandler handler;
	
	protected QuirkHandler() throws Exception
	{
		if(CampaignMain.cm.getBooleanConfig("EnableQuirks"))
		{
			QuirksHandler.initQuirksList();
		}
		
	}
	
	public static QuirkHandler getInstance() 
	{
		if (handler == null) 
		{
			try 
			{
				handler = new QuirkHandler();
			} 
			catch (Exception e) 
			{
				MWLogger.errLog(e);
			}
		}
		return handler;
	}
		
	public void setQuirks(SUnit unit)
	{
		if(CampaignMain.cm.getBooleanConfig("EnableQuirks"))
		{
			unit.getEntity().loadDefaultQuirks();
			MWLogger.debugLog(unit.getModelName() +" "+ unit.getId() +" Quirks: " + returnQuirkList(unit));			
		}
	}
	
	/**
	 * @param unit
	 * @return quirksList
	 */
	public String returnHtmlQuirkList(SUnit unit) 
	{
		StringJoiner quirksList = new StringJoiner("<br>*");
       
        for (Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups(); optionGroups.hasMoreElements();) 
        {
          IOptionGroup group = optionGroups.nextElement();
          if (unit.getEntity().getQuirks().count(group.getKey()) > 0) 
          {
            for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) 
            {
              IOption option = options.nextElement();
              if (option != null && option.booleanValue()) 
              {
                quirksList.add(option.getDisplayableNameWithValue());
              }
            }
          }
        }
        
        if(StringUtil.isNullOrEmpty(quirksList.toString()))
        {
        	quirksList.add("None");
        }
        
		return quirksList.toString();
	}
	
	/**
	 * @param unit
	 * @return quirksList
	 */
	public String returnQuirkList(SUnit unit) 
	{
		StringJoiner quirksList = new StringJoiner("&");
        
        for (Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups(); optionGroups.hasMoreElements();) 
        {
          IOptionGroup group = optionGroups.nextElement();
          if (unit.getEntity().getQuirks().count(group.getKey()) > 0) 
          {
            for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) 
            {
              IOption option = options.nextElement();
              if (option != null && option.booleanValue()) 
              {
                quirksList.add(option.getName());
              }
            }
          }
        }
        
        if(StringUtil.isNullOrEmpty(quirksList.toString()))
        {
        	quirksList.add("None");
        }
        
		return quirksList.toString();
	}
	
	/**
	 * @param unit
	 * @return quirksList
	 */
	public String returnQuirkSave(SUnit unit) 
	{
		if(CampaignMain.cm.getBooleanConfig("EnableQuirks"))
		{
		StringJoiner quirksList = new StringJoiner("!");
		quirksList.add(returnHtmlQuirkList(unit));
		quirksList.add(returnQuirkList(unit));
		
		MWLogger.debugLog(unit.getVerboseModelName() + ": " +quirksList.toString());
		
		return quirksList.toString(); // if a unit has no quirks, it will return a "!"
		}
		
		return " ";
	}
	
    public boolean hasQuirks(SUnit unit)
    {      
        for (Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups(); optionGroups.hasMoreElements();) 
        {
          IOptionGroup group = optionGroups.nextElement();
          if (unit.getEntity().getQuirks().count(group.getKey()) > 0) 
          {
            for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) 
            {
              IOption option = options.nextElement();
              if (option != null && option.booleanValue()) 
              {
                return true;
              }
            }
          }
        }
		return false;
    }
    

	
}


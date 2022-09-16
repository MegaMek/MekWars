/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package server.campaign.util;
import server.campaign.SHouse;

public class HouseRankingHelpContainer implements Comparable<Object>

{
  SHouse faction;
  int amount;

  public HouseRankingHelpContainer(SHouse h)
  {
    this.faction = h;
    amount = 0;
  }

  public void addAmount (int a)
  {
    amount += a;
  }

   public int compareTo(Object o)
  {
    HouseRankingHelpContainer h = (HouseRankingHelpContainer)o;
    if (amount - faction.getInitialHouseRanking() > h.getAmount() - h.getHouse().getInitialHouseRanking())
      return 1;
    else if (amount - faction.getInitialHouseRanking() < h.getAmount() - h.getHouse().getInitialHouseRanking())
      return -1;
    return this.getHouse().compareTo(h.getHouse());
  }
  public int getAmount()
  {
    return amount;
  }
  public void setAmount(int amount)
  {
    this.amount = amount;
  }
  public SHouse getHouse()
  {
    return faction;
  }
  public void setHouse(SHouse h)
  {
    this.faction = h;
  }

}
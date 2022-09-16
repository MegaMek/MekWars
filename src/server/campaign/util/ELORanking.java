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

public class ELORanking
{

  public static double calcWinProp(double PlayerRating, double OponentRating)
  {
    return 1 / (Math.pow(10,((OponentRating - PlayerRating) / 400)) + 1);
  }

  public static double getNewRatingWinner(double WinnerRating,double LoserRating,int KValue)
  {
    return WinnerRating + (KValue*(1-calcWinProp(WinnerRating,LoserRating)));
  }
  public static double getNewRatingLoser(double WinnerRating,double LoserRating,int KValue)
  {
    //K-Value of the loser is one less than the winner, so scale slowly rises
/*    if (KValue > 1)
      KValue--;*/
    return LoserRating + (KValue*(0-calcWinProp(LoserRating,WinnerRating)));
  }
}

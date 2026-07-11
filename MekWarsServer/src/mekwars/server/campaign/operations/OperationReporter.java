	/*
     * MekWars - Copyright (C) 2008
     *
     * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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

    package mekwars.server.campaign.operations;

    import common.Unit;
    import megamek.common.IEntityRemovalConditions;
import megamek.logging.MMLogger;
    import mekwars.server.campaign.CampaignMain;

    public class OperationReporter {
    private static final MMLogger LOGGER = MMLogger.create(OperationReporter.class);

        private OperationReportEntry opData = new OperationReportEntry();
        private java.util.Vector<String> winnerSet = new java.util.Vector<String>();
        private java.util.Vector<String> loserSet = new java.util.Vector<String>();

        private java.util.TreeMap<Integer, String> attackerUnits = new java.util.TreeMap<Integer, String>();
        private java.util.TreeMap<Integer, String> defenderUnits = new java.util.TreeMap<Integer, String>();
        private java.util.TreeMap<String, String> attackerMap = new java.util.TreeMap<String, String>();
        private java.util.TreeMap<String, String> defenderMap = new java.util.TreeMap<String, String>();

        public OperationReporter() {

        }

        public void setWinnersAndLosers(
              java.util.TreeMap<String, server.campaign.SPlayer> winners,
              java.util.TreeMap<String, server.campaign.SPlayer> losers) {
            setWinners(winners);
            setLosers(losers);
        }

        private void setWinners(java.util.TreeMap<String, server.campaign.SPlayer> winners) {

            for (String winner : winners.keySet()) {
                winnerSet.add(winner);
            }
            java.util.Iterator<String> it = winnerSet.iterator();
            int count = 0;
            StringBuilder wNames = new StringBuilder();

            while (it.hasNext()) {
                if (count > 0) {wNames.append(", ");}
                String name = it.next();

                if (name.equalsIgnoreCase("Draw")) {
                    wNames.append("Draw");
                } else {
                    try {
                        wNames.append(winners.get(name).getName());
                    } catch (Exception e) {
                        if (wNames == null) {
                            LOGGER.info("wNames is null");
                        } else if (winners == null) {
                            LOGGER.info("winners is null");
                        } else {
                            LOGGER.info("Winners must have returned a null object");
                            LOGGER.info("looking for: " + name);
                            LOGGER.info("size: " + winners.size());
                            LOGGER.info("conents: " + winners.keySet().toString());
                        }
                    }
                }
                count++;
            }
            opData.setWinnerName(wNames.toString());
        }

        private void setLosers(java.util.TreeMap<String, server.campaign.SPlayer> losers) {
            for (String loser : losers.keySet()) {loserSet.add(loser);}
            java.util.Iterator<String> it = loserSet.iterator();
            int count = 0;
            StringBuilder lNames = new StringBuilder();

            while (it.hasNext()) {
                if (count > 0) {lNames.append(", ");}
                lNames.append(losers.get(it.next()).getName());
                count++;
            }
            opData.setLoserName(lNames.toString());
        }

        public void setAttackerStartBV(int BV) {
            opData.setBV(true, true, BV);
        }

        public void setDefenderStartBV(int BV) {
            opData.setBV(false, true, BV);
        }

        public void setAttackerEndBV(int BV) {
            opData.setBV(true, false, BV);
        }

        public void setDefenderEndBV(int BV) {
            opData.setBV(false, false, BV);
        }

        public void commit() {
            opData.setEndTime(System.currentTimeMillis());

            LOGGER.info("Operation Finished: ");
            LOGGER.info("  OpType: " + opData.getOpType());
            LOGGER.info("  Planet: " +
                                      opData.getPlanet() +
                                      ", Terrain: " +
                                      opData.getTerrain() +
                                      ", Theme: " +
                                      opData.getTheme());
            LOGGER.info("  Attacker(s): " +
                                      opData.getAttackers() +
                                      " (" +
                                      opData.getAttackerSize() +
                                      " units)  --  Defender(s): " +
                                      opData.getDefenders() +
                                      " (" +
                                      opData.getDefenderSize() +
                                      " units)");
            LOGGER.info("  BVs: Attacker: " +
                                      opData.getAttackerStartBV() +
                                      " / " +
                                      opData.getAttackerEndBV() +
                                      "  --  Defender: " +
                                      opData.getDefenderStartBV() +
                                      " / " +
                                      opData.getDefenderEndBV());
            LOGGER.info("  Attacker Won: " + Boolean.toString(opData.attackerIsWinner()));
            LOGGER.info("  Winner(s): " + opData.getWinners() + "  --  Loser(s): " + opData.getLosers());
            LOGGER.info("  Game Length: " + opData.getHumanReadableGameLength());
        }

        public void closeOperation(boolean draw, boolean attackerWon) {
            opData.setDrawGame(draw);
            opData.setAttackerWon(attackerWon);

        }

        public void setUpOperation(String operationName, String planetName, String terrainName, String themeName) {
            setPlanetInfo(planetName, terrainName, themeName);
            opData.setOpType(operationName);
            opData.setStartTime(System.currentTimeMillis());
        }

        public void setPlanetInfo(String pName, String tName, String thName) {
            opData.setPlanetInfo(pName, tName, thName);
        }

        public void addAttacker(String playerName, int armyID) {
            attackerMap.put(playerName,
                  CampaignMain.campaignMain.getPlayer(playerName).getHouseFightingFor().getName());
            String playerString = playerName + " (" + attackerMap.get(playerName) + ")";
            server.campaign.SArmy army = CampaignMain.campaignMain.getPlayer(playerName).getArmy(armyID);
            if (army != null) {
                opData.addStartingBV(true, army.getBV());
                addArmy(true, army);
                String s = opData.getAttackers();
                if (s.length() == 0) {opData.setAttackerName(playerString);} else {
                    opData.setAttackerName(s + ", " + playerString);
                }
            }
        }

        public void addArmy(boolean attackerArmy, server.campaign.SArmy army) {
            // Keep a list of units for each side
            int numUnits = army.getAmountOfUnits();
            if (attackerArmy) {opData.setAttackerSize(numUnits);} else {opData.setDefenderSize(numUnits);}

            for (Unit currU : army.getUnits()) {
                int ID = currU.getId();
                String model = ((server.campaign.SUnit) currU).getModelName();
                if (attackerArmy) {attackerUnits.put(ID, model);} else {defenderUnits.put(ID, model);}
            }
        }

        public void addDefender(String playerName, int armyID) {
            defenderMap.put(playerName,
                  CampaignMain.campaignMain.getPlayer(playerName).getHouseFightingFor().getName());
            String playerString = playerName + " (" + defenderMap.get(playerName) + ")";
            server.campaign.SArmy army = CampaignMain.campaignMain.getPlayer(playerName).getArmy(armyID);
            if (army != null) {
                opData.addStartingBV(false, army.getBV());
                addArmy(false, army);
                String s = opData.getDefenders();
                if (s.length() == 0) {opData.setDefenderName(playerString);} else {
                    opData.setDefenderName(s + ", " + playerString);
                }
            }
        }

        public void addEndingUnit(server.campaign.SUnit unit, int removalReason) {
            if (removalReason == IEntityRemovalConditions.REMOVE_DEVASTATED ||
                      removalReason == IEntityRemovalConditions.REMOVE_PUSHED ||
                      removalReason == IEntityRemovalConditions.REMOVE_EJECTED ||
                      removalReason == IEntityRemovalConditions.REMOVE_NEVER_JOINED) {
                // Nothing to do here, as the unit doesn't count for BV
                return;
            }
            // The unit is still part of an army.  Add it's current BV to the ending BVs
            addEndingBV(unit.getId(), unit.calcBV());
        }

        private void addEndingBV(int unitID, int BV) {
            // First, figure if it's an attacking or defending unit
            if (attackerUnits.containsKey(unitID)) {opData.addEndingBV(true, BV);} else if (defenderUnits.containsKey(
                  unitID)) {opData.addEndingBV(false, BV);}
        }
    }

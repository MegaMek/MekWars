package mekwars.server.campaign.pilot;

import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import server.campaign.pilot.skills.*;

public class SPilotSkills {
    private static java.util.Hashtable<Integer, SPilotSkill> pilotSkills = new java.util.Hashtable<Integer, SPilotSkill>();


    public static void initializePilotSkills() {
        // PilotSkills
        pilotSkills.put(PilotSkill.DodgeManeuverSkillID, (new DodgeManeuverSkill(PilotSkill.DodgeManeuverSkillID)));
        pilotSkills.put(PilotSkill.ManeuveringAceSkillID, (new ManeuveringAceSkill(PilotSkill.ManeuveringAceSkillID)));
        pilotSkills.put(PilotSkill.MeleeSpecialistSkillID,
              (new MeleeSpecialistSkill(PilotSkill.MeleeSpecialistSkillID)));
        pilotSkills.put(PilotSkill.PainResistanceSkillID, (new PainResistanceSkill(PilotSkill.PainResistanceSkillID)));
        pilotSkills.put(PilotSkill.AstechSkillID, (new AstechSkill(PilotSkill.AstechSkillID)));
        pilotSkills.put(PilotSkill.NaturalAptitudeGunnerySkillID,
              (new NaturalAptitudeGunnerySkill(PilotSkill.NaturalAptitudeGunnerySkillID)));
        pilotSkills.put(PilotSkill.NaturalAptitudePilotingSkillID,
              (new NaturalAptitudePilotingSkill(PilotSkill.NaturalAptitudePilotingSkillID)));
        pilotSkills.put(PilotSkill.IronManSkillID, (new IronManSkill(PilotSkill.IronManSkillID)));
        pilotSkills.put(PilotSkill.GunneryBallisticSkillID,
              (new GunneryBallisticSkill(PilotSkill.GunneryBallisticSkillID)));
        pilotSkills.put(PilotSkill.GunneryLaserSkillID, (new GunneryLaserSkill(PilotSkill.GunneryLaserSkillID)));
        pilotSkills.put(PilotSkill.GunneryMissileSkillID, (new GunneryMissileSkill(PilotSkill.GunneryMissileSkillID)));
        pilotSkills.put(PilotSkill.TacticalGeniusSkillID, (new TacticalGeniusSkill(PilotSkill.TacticalGeniusSkillID)));
        pilotSkills.put(PilotSkill.WeaponSpecialistSkillID,
              (new WeaponSpecialistSkill(PilotSkill.WeaponSpecialistSkillID)));
        pilotSkills.put(PilotSkill.SurvivalistSkillID, (new SurvivalistSkill(PilotSkill.SurvivalistSkillID)));
        pilotSkills.put(PilotSkill.TraitID, (new TraitSkill(PilotSkill.TraitID)));
        pilotSkills.put(PilotSkill.EnhancedInterfaceID, (new EnhancedInterfaceSkill(PilotSkill.EnhancedInterfaceID)));
        pilotSkills.put(PilotSkill.QuickStudyID, (new QuickStudySkill(PilotSkill.QuickStudyID)));
        pilotSkills.put(PilotSkill.GiftedID, (new GiftedSkill(PilotSkill.GiftedID)));
        pilotSkills.put(PilotSkill.MedTechID, (new MedTechSkill(PilotSkill.MedTechID)));
        pilotSkills.put(PilotSkill.EdgeSkillID, (new EdgeSkill(PilotSkill.EdgeSkillID)));
        pilotSkills.put(PilotSkill.ClanPilotTraingID, (new ClanPilotTrainingSkill(PilotSkill.ClanPilotTraingID)));
        pilotSkills.put(PilotSkill.VDNIID, (new VDNI(PilotSkill.VDNIID)));
        pilotSkills.put(PilotSkill.BufferedVDNIID, (new BufferedVDNI(PilotSkill.BufferedVDNIID)));
        pilotSkills.put(PilotSkill.PainShuntID, (new PainShunt(PilotSkill.PainShuntID)));
    }

    public static SPilotSkill getRandomSkill(SPilot p, int unitType) {
        int total = 0;

        java.util.Iterator<SPilotSkill> it = pilotSkills.values().iterator();
        java.util.Hashtable<Integer, Integer> skilltable = new java.util.Hashtable<Integer, Integer>();
        if (p.getSkills().has(PilotSkill.TraitID)) {
            // SPilotSkill skill =
            // (SPilotSkill)p.getSkills().getPilotSkill(SPilotSkill.TraitID);
            String trait = p.getTraitName();
            if (trait.indexOf("*") > -1) {
                trait = trait.substring(0, trait.indexOf("*"));
            }
            java.util.Vector<String> traitsList = CampaignMain.campaignMain.getFactionTraits(p.getCurrentFaction());
            traitsList.trimToSize();
            for (String traitNames : traitsList) {
                java.util.StringTokenizer traitName = new java.util.StringTokenizer(traitNames, "*");
                String traitString = traitName.nextToken();
                if (traitString.equalsIgnoreCase(trait)) {
                    while (traitName.hasMoreElements()) {
                        int traitid = Integer.parseInt(traitName.nextToken());
                        int traitMod = Integer.parseInt(traitName.nextToken());
                        skilltable.put(traitid, traitMod);
                    }
                }
            }
        }

        // check for trait mods and add them
        while (it.hasNext()) {
            SPilotSkill skill = it.next();
            total += skill.getChance(unitType, p);
        }

        if (total == 0) {
            return null;
        }
        /*
         * int rnd = 1; if (total > 1) rnd = getR().nextInt(total) + 1;
         */
        it = pilotSkills.values().iterator();
        java.util.Vector<SPilotSkill> skillBuilder = new java.util.Vector<SPilotSkill>(total, 1);

        try {
            while (it.hasNext()) {
                SPilotSkill skill = it.next();
                int chance = skill.getChance(unitType, p);
                if (skilltable.get(skill.getId()) != null) {
                    chance += skilltable.get(skill.getId());
                }

                for (int pos = 0; pos < chance; pos++) {
                    skillBuilder.add(skill);
                }
                skillBuilder.trimToSize();
                /*
                 * //MWLogger.errLog("Pilot: "+p.getName()+" Skill:
                 * "+skill.getName()+" Rnd "+rnd+ " chance: "+chance); if ( rnd
                 * <= chance ) return skill; //else rnd -=
                 * skill.getChance(unitType,p);
                 */
            }

            return skillBuilder.elementAt(CampaignMain.campaignMain.getRandomNumber(skillBuilder.size()));
        } catch (Exception ex) {
            MWLogger.errLog("Problems during skill earning! Skill Table Size = " +
                                  skillBuilder.size() +
                                  " total = " +
                                  total);
            return null;
        }
    }

    /**
     * Create a skill from a string. Used by CreateUnitCommand.
     */
    public static SPilotSkill getPilotSkill(String skill) {

        for (SPilotSkill pSkill : pilotSkills.values()) {
            if (pSkill.getName().equalsIgnoreCase(skill) || pSkill.getAbbreviation().equalsIgnoreCase(skill)) {
                return pSkill;
            }
        }

        return null;
    }

    /**
     * Get a pilot skill by ID number. Used to unstring SPilots in pfiles.
     */
    public static SPilotSkill getPilotSkill(int id) {
        return pilotSkills.get(id);
    }
}

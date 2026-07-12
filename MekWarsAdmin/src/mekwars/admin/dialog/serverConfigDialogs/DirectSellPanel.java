/*
 * MekWars - Copyright (C) 2011
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package mekwars.admin.dialog.serverConfigDialogs;

import java.io.Serial;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import mekwars.common.util.SpringLayoutHelper;


/**
 * @author Spork
 * @author jtighe
 */
public class DirectSellPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 3795966693108854838L;

    public DirectSellPanel() {
        super();

        /*
         * CONSTRUCT MEZZO/PriceMod PANEL
         */
        JPanel MekSpring = new JPanel(new SpringLayout());
        JPanel VehicleSpring = new JPanel(new SpringLayout());
        JPanel InfantrySpring = new JPanel(new SpringLayout());
        JPanel BattleArmorSpring = new JPanel(new SpringLayout());
        JPanel ProtoMekSpring = new JPanel(new SpringLayout());
        JPanel AeroSpring = new JPanel(new SpringLayout());

        JPanel buySellSpring = new JPanel(new SpringLayout());

        JTextField baseTextField = new JTextField(5);
        MekSpring.add(new JLabel("Light Mek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Mek.");
        baseTextField.setName("SellDirectLightMekPrice");
        MekSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        MekSpring.add(new JLabel("Medium Mek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Mek.");
        baseTextField.setName("SellDirectMediumMekPrice");
        MekSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        MekSpring.add(new JLabel("Heavy Mek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Mek.");
        baseTextField.setName("SellDirectHeavyMekPrice");
        MekSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        MekSpring.add(new JLabel("Assault Mek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Mek.");
        baseTextField.setName("SellDirectAssaultMekPrice");
        MekSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(MekSpring, 4, 2);

        baseTextField = new JTextField(5);
        VehicleSpring.add(new JLabel("Light Vehicle PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Vehicle.");
        baseTextField.setName("SellDirectLightVehiclePrice");
        VehicleSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        VehicleSpring.add(new JLabel("Medium Vehicle PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Vehicle.");
        baseTextField.setName("SellDirectMediumVehiclePrice");
        VehicleSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        VehicleSpring.add(new JLabel("Heavy Vehicle PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Vehicle.");
        baseTextField.setName("SellDirectHeavyVehiclePrice");
        VehicleSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        VehicleSpring.add(new JLabel("Assault Vehicle PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Vehicle.");
        baseTextField.setName("SellDirectAssaultVehiclePrice");
        VehicleSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(VehicleSpring, 4, 2);

        baseTextField = new JTextField(5);
        InfantrySpring.add(new JLabel("Light Infantry PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Infantry.");
        baseTextField.setName("SellDirectLightInfantryPrice");
        InfantrySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        InfantrySpring.add(new JLabel("Medium Infantry PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Infantry.");
        baseTextField.setName("SellDirectMediumInfantryPrice");
        InfantrySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        InfantrySpring.add(new JLabel("Heavy Infantry PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Infantry.");
        baseTextField.setName("SellDirectHeavyInfantryPrice");
        InfantrySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        InfantrySpring.add(new JLabel("Assault Infantry PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Infantry.");
        baseTextField.setName("SellDirectAssaultInfantryPrice");
        InfantrySpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(InfantrySpring, 4, 2);

        baseTextField = new JTextField(5);
        BattleArmorSpring.add(new JLabel("Light BattleArmor PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a BattleArmor.");
        baseTextField.setName("SellDirectLightBattleArmorPrice");
        BattleArmorSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        BattleArmorSpring.add(new JLabel("Medium BattleArmor PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a BattleArmor.");
        baseTextField.setName("SellDirectMediumBattleArmorPrice");
        BattleArmorSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        BattleArmorSpring.add(new JLabel("Heavy BattleArmor PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a BattleArmor.");
        baseTextField.setName("SellDirectHeavyBattleArmorPrice");
        BattleArmorSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        BattleArmorSpring.add(new JLabel("Assault BattleArmor PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a BattleArmor.");
        baseTextField.setName("SellDirectAssaultBattleArmorPrice");
        BattleArmorSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(BattleArmorSpring, 4, 2);

        baseTextField = new JTextField(5);
        AeroSpring.add(new JLabel("Light Aero PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Aero.");
        baseTextField.setName("SellDirectLightAeroPrice");
        AeroSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        AeroSpring.add(new JLabel("Medium Aero PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Aero.");
        baseTextField.setName("SellDirectMediumAeroPrice");
        AeroSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        AeroSpring.add(new JLabel("Heavy Aero PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Aero.");
        baseTextField.setName("SellDirectHeavyAeroPrice");
        AeroSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        AeroSpring.add(new JLabel("Assault Aero PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a Aero.");
        baseTextField.setName("SellDirectAssaultAeroPrice");
        AeroSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(AeroSpring, 4, 2);

        baseTextField = new JTextField(5);
        ProtoMekSpring.add(new JLabel("Light ProtoMek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a ProtoMek.");
        baseTextField.setName("SellDirectLightProtoMekPrice");
        ProtoMekSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        ProtoMekSpring.add(new JLabel("Medium ProtoMek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a ProtoMek.");
        baseTextField.setName("SellDirectMediumProtoMekPrice");
        ProtoMekSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        ProtoMekSpring.add(new JLabel("Heavy ProtoMek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a ProtoMek.");
        baseTextField.setName("SellDirectHeavyProtoMekPrice");
        ProtoMekSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        ProtoMekSpring.add(new JLabel("Assault ProtoMek PriceMod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount added to PriceMod to direct sell a ProtoMek.");
        baseTextField.setName("SellDirectAssaultProtoMekPrice");
        ProtoMekSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(ProtoMekSpring, 4, 2);

        buySellSpring.add(MekSpring);
        buySellSpring.add(VehicleSpring);
        buySellSpring.add(InfantrySpring);
        buySellSpring.add(BattleArmorSpring);
        buySellSpring.add(ProtoMekSpring);
        buySellSpring.add(AeroSpring);

        SpringLayoutHelper.setupSpringGrid(buySellSpring, 2, 3);

        JPanel buySellSpring2 = new JPanel();
        buySellSpring2.setLayout(new BoxLayout(buySellSpring2, BoxLayout.Y_AXIS));

        // finalize layout
        JCheckBox baseCheckBox = new JCheckBox("Use Direct Sell");
        baseCheckBox.setName("UseDirectSell");

        buySellSpring2.add(baseCheckBox);
        buySellSpring2.add(buySellSpring);

        // SpringLayoutHelper.setupSpringGrid(buySellSpring2 , 1, 3);
        add(buySellSpring2);
    }
}

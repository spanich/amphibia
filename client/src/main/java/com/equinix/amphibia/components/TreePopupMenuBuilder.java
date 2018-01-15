/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia.components;

import static com.equinix.amphibia.Amphibia.getUserPreferences;
import static com.equinix.amphibia.components.TreeCollection.TYPE.*;

import com.equinix.amphibia.Amphibia;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import net.sf.json.JSONArray;


/**
 *
 * @author dgofman
 */
public final class TreePopupMenuBuilder implements ActionListener {

    private final Amphibia amphibia;
    private final MainPanel mainPanel;
    private final ResourceAddDialog resourcesDialog;

    private final ResourceBundle bundle;

    private TreeIconNode selectedNode;
    
    private final Preferences userPreferences = getUserPreferences();
    
    private static final Logger logger = Logger.getLogger(TreePopupMenuBuilder.class.getName());

    public TreePopupMenuBuilder(Amphibia amphibia, MainPanel mainPanel) {
        this.amphibia = amphibia;
        this.mainPanel = mainPanel;
        bundle = Amphibia.getBundle();
        resourcesDialog = new ResourceAddDialog(mainPanel);
    }

    @SuppressWarnings("NonPublicExported")
    public JPopupMenu createPopupMenu(TreeIconNode selectedNode) {
        this.selectedNode = selectedNode;

        JPopupMenu popup = new JPopupMenu();
        TreeIconNode.TreeIconUserObject userObject = selectedNode.getTreeIconUserObject();

        switch (userObject.getType()) {
            case PROJECT:
                addMenu(popup, "mnuRename", "RENAME:PROJECT");
                if (userObject.isEnabled()) {
                    addMenu(popup, "mnuReload", "RELOAD");
                    addMenu(popup, "mnuCloseProject", "CLOSE");
                } else {
                    addMenu(popup, "mnuOpenProject", "OPEN");
                }
                addMenu(popup, "mnuDeleteProject", "DELETE");
                break;
            case INTERFACE:
                addMenu(popup, "mnuRename", "RENAME:INTERFACE");
                break;
            default:
                break;

        }
        return popup;
    }

    private JMenuItem addMenu(JComponent parent, String label, String actionCommand) {
        return addMenu(parent, label, actionCommand, true);
    }

    private JMenuItem addMenu(JComponent parent, String label, String actionCommand, boolean localize) {
        JMenuItem menu = new JMenuItem(localize ? bundle.getString(label) : label);
        menu.setActionCommand(actionCommand);
        menu.setMargin(new java.awt.Insets(0, 0, 0, 25));
        menu.addActionListener(this);
        parent.add(menu);
        return menu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JMenuItem menuitem = (JMenuItem) e.getSource();
        String[] commands = menuitem.getActionCommand().split(":");
        TreeCollection collection = selectedNode.getCollection();
        switch (commands[0]) {
            case "RENAME":
                mainPanel.history.renameResource(commands[1].equals("PROJECT"), collection);
                break;
            case "CLOSE":
                mainPanel.openCloseProject(false);
                break;
            case "OPEN":
                mainPanel.openCloseProject(true);
                break;
            case "RELOAD":
                mainPanel.reloadCollection(collection);
                break;
            case "DELETE":
                int dialogResult = JOptionPane.showConfirmDialog(null, bundle.getString("tip_deleting"), bundle.getString("deleteResources"), JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    mainPanel.deleteProject(collection);
                    JSONArray list = JSONArray.fromObject(userPreferences.get(Amphibia.P_PROJECT_UUIDS, "[]"));
                    for (int i = 0; i < list.size(); i++) {
                        if (collection.getUUID().equals(list.getString(i))) {
                            list.remove(i);
                            userPreferences.put(Amphibia.P_PROJECT_UUIDS, list.toString());
                            userPreferences.remove(Amphibia.P_PROJECT + collection.getUUID());
                            break;
                        }
                    }
                }
                break;
        }
    }
}

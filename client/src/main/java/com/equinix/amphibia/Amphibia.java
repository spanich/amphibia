/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.equinix.amphibia;

import com.equinix.amphibia.components.FindDialog;
import com.equinix.amphibia.components.HelpDialog;
import com.equinix.amphibia.components.MainPanel;
import com.equinix.amphibia.components.PreferenceDialog;
import com.equinix.amphibia.components.ProjectDialog;
import com.equinix.amphibia.components.TipDialog;
import com.equinix.amphibia.components.TreeCollection;
import com.equinix.amphibia.components.TreeIconNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author dgofman
 */
public class Amphibia extends JFrame {

    public static final Amphibia instance = new Amphibia();
    public static final Color OVERLAY_BG_COLOR = new Color(246, 246, 246, 200);

    private static final Logger logger = Logger.getLogger(Amphibia.class.getName());

    public static final String P_PROJECT = "project_";
    public static final String P_PROJECT_UUIDS = "projects";
    public static final String P_RECENT_PROJECTS = "recent";
    public static final String P_SELECTED_NODE = "selected";
    public static final String P_LAST_DIRECTORY = "lastdir";
    public static final String P_DIVIDER = "divider";
    public static final String P_MAXIMIZED = "maximized";
    public static final String P_APP_BOUNDS = "bounds";
    public static final String P_HELP_BOUNDS = "help";
    public static final String P_LOOKANDFEEL = "lookAndFeel";
    public static final String P_MENU_VIEW = "view";
    public static final String P_GROUP_ID = "groupId";
    public static final String P_LOCALE = "locale";
    public static final String P_HISTORY = "history";
    public static final String P_VIEW_TABS = "tabs";
    public static final String P_SKIPPED_TEST = "skipped";
    public static final String P_SHOW_EXPERT_TIP = "expert";
    public static final String P_SHOW_DEBUGGER_TIP = "debugger";
    public static final String P_SWITCH_DEBUGGER = "switchDebug";
    public static final String P_SWITCH_PROBLEMS = "switchProblems";
    public static final String P_SWITCH_CONSOLE = "switchConsole";
    public static final String P_INHERIT_PROPERTIES = "inheritProperties";
    public static final String P_CONN_TIMEOUT = "ctimeout";
    public static final String P_READ_TIMEOUT = "rtimeout";
    public static final String P_CONTINUE_ON_ERROR = "onerror";

    public static final int TAB_PROBLEMS = 0;
    public static final int TAB_RAW = 1;
    public static final int TAB_CONSOLE = 2;
    public static final int TAB_HISTORY = 3;

    public static final String VERSION = "1.0";

    public static final Preferences userPreferences = getUserPreferences();
    
    public final ImageIcon icon;
    public final ImageIcon waitIcon;

    private boolean isExpertView;
    private ResourceBundle bundle;
    private FindDialog findDialog;
    private HelpDialog helpDialog;
    private TipDialog tipDialog;

    private ProjectDialog projectDialog;
    private PreferenceDialog preferenceDialog;

    public static void main(String args[]) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
        java.awt.EventQueue.invokeLater(() -> {
            instance.init();
            instance.setAlwaysOnTop(true);
            instance.toFront();
            instance.requestFocus();
            instance.setAlwaysOnTop(false);
        });
    }

    public Amphibia() {
        super();
        icon = new ImageIcon(Amphibia.class.getResource("/com/equinix/amphibia/icons/logo_16.png"));
        waitIcon = new ImageIcon(Amphibia.class.getResource("/com/equinix/amphibia/icons/ajax-loader.gif"));
    }

    /**
     * Creates new form NewJFrame
     */
    public void init() {

        String[] arr = userPreferences.get(P_LOCALE, "").split("_");
        if (arr.length == 2) {
            Locale.setDefault(new Locale(arr[0], arr[1]));
        }

        bundle = Amphibia.getBundle();

        try {
            String userLF = userPreferences.get(Amphibia.P_LOOKANDFEEL, UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(userLF);
            UIManager.put("OptionPane.title", bundle.getString("title"));
            UIManager.put("OptionPane.yesButtonText", bundle.getString("yes"));
            UIManager.put("OptionPane.noButtonText", bundle.getString("no"));
            UIManager.put("OptionPane.okButtonText", bundle.getString("ok"));
            UIManager.put("OptionPane.cancelButtonText", bundle.getString("cancel"));
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        initComponents();

        String[] tabs = userPreferences.get(P_VIEW_TABS, "1111").split("");
        for (int i = 0; i < tabs.length; i++) {
            showHideTab(i, "1".equals(tabs[i]));
        }

        isExpertView = userPreferences.getBoolean(P_MENU_VIEW, false);
        mnuExpert.setSelected(isExpertView);

        lpnLayer.add(pnlWaitOverlay, 0, 0);
        pnlWaitOverlay.setVisible(false);
        setLocationRelativeTo(null);
        setIconImage(icon.getImage());
        mainPanel.setParent(this);

        projectDialog = new ProjectDialog(mainPanel);
        findDialog = new FindDialog(mainPanel);
        preferenceDialog = new PreferenceDialog(mainPanel);
        helpDialog = new HelpDialog(mainPanel);
        tipDialog = new TipDialog(mainPanel);

        preferenceDialog.txtGroupId.setText(userPreferences.get(P_GROUP_ID, "com.example"));
        inheritProp.setSelected(userPreferences.getBoolean(Amphibia.P_INHERIT_PROPERTIES, true));

        TreeCollection.initializeCollections(mainPanel, userPreferences);
        String projects = userPreferences.get(P_RECENT_PROJECTS, null);
        if (projects != null) {
            try {
                JSONArray data = (JSONArray) IO.toJSON(projects);
                createRecentProjectMenu(data);
            } catch (Exception ex) {
                mainPanel.addError(ex);
            }
        }
        mainPanel.reloadAll();
        mainPanel.runner.openReport();

        this.addComponentListener(new ComponentAdapter() {
            Timer timer = new Timer();

            @Override
            public void componentResized(ComponentEvent evt) {
                updatePreferences(timer, () -> {
                    Amphibia.setBounds(Amphibia.this, P_APP_BOUNDS);
                    userPreferences.putBoolean(P_MAXIMIZED, Amphibia.this.getExtendedState() == JFrame.MAXIMIZED_BOTH);
                });
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updatePreferences(timer, () -> {
                    Amphibia.setBounds(Amphibia.this, P_APP_BOUNDS);
                });
            }
        });
        if (userPreferences.getBoolean(P_MAXIMIZED, false)) {
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            Amphibia.getBounds(this, P_APP_BOUNDS);
        }
        setVisible(true);
    }

    public void showHideTab(int index, boolean b) {
        switch (index) {
            case TAB_PROBLEMS:
                mainPanel.editor.showHideTab(index, b);
                mnuProblems.setSelected(b);
                break;
            case TAB_RAW:
                mainPanel.editor.showHideTab(index, b);
                mnuRaw.setSelected(b);
                break;
            case TAB_CONSOLE:
                mainPanel.editor.showHideTab(index, b);
                mnuConsole.setSelected(b);
                break;
            case TAB_HISTORY:
                mainPanel.editor.showHideTab(index, b);
                mnuHistory.setSelected(b);
                break;
            default:
                return;
        }
        String[] tabs = userPreferences.get(P_VIEW_TABS, "1111111").split("");
        tabs[index] = (b ? "1" : "0");
        userPreferences.put(P_VIEW_TABS, String.join("", tabs));
    }

    public static void updatePreferences(Timer timer, Runnable runnable) {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, 500);
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("com/equinix/amphibia/messages");
    }
    
    public static boolean isExpertView() {
        return instance.isExpertView;
    }

    public void createRecentProjectMenu(JSONArray list) {
        menuRecentProject.removeAll();
        for (int i = 0; i < list.size(); i++) {
            JSONObject project = list.getJSONObject(i);
            JMenuItem menu = new JMenuItem(project.getString("name"));
            menu.setToolTipText(project.getString("path"));
            menu.addActionListener((ActionEvent evt) -> {
                try {
                    File file = new File(project.getString("path"));
                    TreeCollection selectedProject = mainPanel.getSelectedProject(file);
                    selectedProject.setUID(project.getString("uuid"));
                    selectedProject.setProjectName(project.getString("name"));
                    selectedProject.setProjectFile(file);
                    mainPanel.loadProject(selectedProject);
                } catch (Exception e) {
                    mainPanel.addError(e);
                }
            });
            menuRecentProject.add(menu);
        }
    }

    public static Preferences getUserPreferences() {
        return Preferences.userNodeForPackage(Amphibia.class);
    }

    public static void setText(JTextComponent txt, JScrollPane sb, String text) {
        if (text != null) {
            txt.setText(text);
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            sb.getVerticalScrollBar().setValue(0);
            sb.getHorizontalScrollBar().setValue(0);
        });
    }

    public static JDialog createDialog(JOptionPane optionPane, boolean isResizable) {
        JDialog dialog = optionPane.createDialog(null, UIManager.getString("OptionPane.title"));
        Frame frame = (Frame) dialog.getParent();
        frame.setIconImage(Amphibia.instance.icon.getImage());
        dialog.setResizable(isResizable);
        return dialog;
    }

    public static JDialog createDialog(Object form, Object[] options, boolean isResizable) {
        JOptionPane optionPane = new JOptionPane(form);
        optionPane.setOptions(options);
        return createDialog(optionPane, isResizable);
    }

    public static void setDefaultHTMLStyles(JEditorPane editor) {
        HTMLEditorKit htmlEditorKit = (HTMLEditorKit) editor.getEditorKit();
        StyleSheet styleSheet = htmlEditorKit.getStyleSheet();
        styleSheet.addRule("ul{list-style-position: inside; margin: 0px 20px;");
        styleSheet.addRule("p{margin: 2px;}");
        styleSheet.addRule("div{white-space: nowrap;}");
        styleSheet.addRule("li{white-space: nowrap}");
    }

    public static void setWaitOverlay(boolean b) {
        if (b) {
            instance.showWaitOverlay();
        } else {
            instance.hideWaitOverlay();
        }
    }

    public void showWaitOverlay() {
        lblAnimation.setLocation((pnlWaitOverlay.getWidth() - lblAnimation.getWidth()) / 2, (pnlWaitOverlay.getHeight() - lblAnimation.getHeight()) / 2);
        pnlWaitOverlay.setVisible(true);
    }

    public void hideWaitOverlay() {
        pnlWaitOverlay.setVisible(false);
    }

    public void openProjectDialog(TreeCollection selectedProject) {
        projectDialog.openDialog(selectedProject);
    }

    public void enableSave(Boolean b) {
        mnuSave.setEnabled(b);
        mnuSaveAs.setEnabled(b);
    }

    public void enableUndo(Boolean b) {
        mnuUndo.setEnabled(b);
        tlbUndo.setEnabled(b);
    }

    public void enableRedo(Boolean b) {
        mnuRedo.setEnabled(b);
        tlbRedo.setEnabled(b);
    }

    public void openTipDialog(String msgKey, String preferenceKey) {
        tipDialog.openDialog(bundle.getString(msgKey), preferenceKey);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlWaitOverlay = new JPanel();
        lblAnimation = new JLabel();
        rbgMnuView = new ButtonGroup();
        tlbTop = new JToolBar();
        btnCreate = new JButton();
        tlbOpen = new JButton();
        tlbSave = new JButton();
        spr1 = new JToolBar.Separator();
        tlbUndo = new JButton();
        tlbRedo = new JButton();
        spr2 = new JToolBar.Separator();
        tlbRefresh = new JButton();
        tlbFind = new JButton();
        spr3 = new JToolBar.Separator();
        tlbRun = new JButton();
        btnPause = new JToggleButton();
        btnStop = new JButton();
        spr4 = new JToolBar.Separator();
        btnReport = new JButton();
        lpnLayer = new JLayeredPane();
        mainPanel = new MainPanel();
        mnbTop = new JMenuBar();
        mnuFile = new JMenu();
        mnuNew = new JMenu();
        mnuNewProject = new JMenuItem();
        mnuRulesFile = new JMenuItem();
        mnuEmptyProject = new JMenuItem();
        mnuOpenProject = new JMenuItem();
        menuRecentProject = new JMenu();
        sprProject = new JPopupMenu.Separator();
        mnuImport = new JMenu();
        mnuImportSoap = new JMenuItem();
        mnuImportPostman = new JMenuItem();
        mnuSave = new JMenuItem();
        mnuSaveAs = new JMenuItem();
        sprExport = new JPopupMenu.Separator();
        mnuRefresh = new JMenuItem();
        mnuPreferences = new JMenuItem();
        sprExit = new JPopupMenu.Separator();
        mnuExit = new JMenuItem();
        mnuEdit = new JMenu();
        mnuUndo = new JMenuItem();
        mnuRedo = new JMenuItem();
        sprFind = new JPopupMenu.Separator();
        mnuFind = new JMenuItem();
        mnuProject = new JMenu();
        mnuRename = new JMenuItem();
        mnuReload = new JMenuItem();
        mnuExport = new JMenu();
        mnuSoap = new JMenuItem();
        mnuReady = new JMenuItem();
        mnuPostman = new JMenuItem();
        mnuMocha = new JMenuItem();
        mnuJunit = new JMenuItem();
        mnuSwagger = new JMenuItem();
        spr6 = new JPopupMenu.Separator();
        mnuReport = new JMenuItem();
        mnuRules = new JMenuItem();
        spr5 = new JPopupMenu.Separator();
        mnuClose = new JMenuItem();
        mnuOpen = new JMenuItem();
        mnuView = new JMenu();
        mnuUser = new JRadioButtonMenuItem();
        mnuExpert = new JRadioButtonMenuItem();
        spr7 = new JPopupMenu.Separator();
        inheritProp = new JCheckBoxMenuItem();
        spr8 = new JPopupMenu.Separator();
        mnuProblems = new JCheckBoxMenuItem();
        mnuRaw = new JCheckBoxMenuItem();
        mnuConsole = new JCheckBoxMenuItem();
        mnuHistory = new JCheckBoxMenuItem();
        mnuHelp = new JMenu();
        mnuHelpContent = new JMenuItem();
        mnuAbout = new JMenuItem();

        pnlWaitOverlay.setBackground(OVERLAY_BG_COLOR);
        pnlWaitOverlay.setLayout(null);

        lblAnimation.setHorizontalAlignment(SwingConstants.CENTER);
        lblAnimation.setIcon(waitIcon);
        lblAnimation.setOpaque(true);
        pnlWaitOverlay.add(lblAnimation);
        lblAnimation.setBounds(0, 0, 60, 0);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        ResourceBundle bundle = ResourceBundle.getBundle("com/equinix/amphibia/messages"); // NOI18N
        setTitle(bundle.getString("title")); // NOI18N
        setMinimumSize(new Dimension(90, 140));
        setPreferredSize(new Dimension(1300, 690));

        tlbTop.setBorder(null);
        tlbTop.setRollover(true);

        btnCreate.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/new_16.png"))); // NOI18N
        btnCreate.setToolTipText(bundle.getString("mnuCloseProject")); // NOI18N
        btnCreate.setFocusable(false);
        btnCreate.setHorizontalTextPosition(SwingConstants.CENTER);
        btnCreate.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnCreate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuNewProjectActionPerformed(evt);
            }
        });
        tlbTop.add(btnCreate);

        tlbOpen.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/open_project_16.png"))); // NOI18N
        tlbOpen.setToolTipText(bundle.getString("mnuOpenProject")); // NOI18N
        tlbOpen.setFocusable(false);
        tlbOpen.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbOpen.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuOpenProjectActionPerformed(evt);
            }
        });
        tlbTop.add(tlbOpen);

        tlbSave.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/save_icon_16.png"))); // NOI18N
        tlbSave.setToolTipText(bundle.getString("mnuSave")); // NOI18N
        tlbSave.setFocusable(false);
        tlbSave.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbSave.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuSaveActionPerformed(evt);
            }
        });
        tlbTop.add(tlbSave);
        tlbTop.add(spr1);

        tlbUndo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/undo-16.png"))); // NOI18N
        tlbUndo.setToolTipText(bundle.getString("mnuUndo")); // NOI18N
        tlbUndo.setEnabled(false);
        tlbUndo.setFocusable(false);
        tlbUndo.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbUndo.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbUndo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuUndoActionPerformed(evt);
            }
        });
        tlbTop.add(tlbUndo);

        tlbRedo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/redo-16.png"))); // NOI18N
        tlbRedo.setToolTipText(bundle.getString("mnuRedo")); // NOI18N
        tlbRedo.setEnabled(false);
        tlbRedo.setFocusable(false);
        tlbRedo.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbRedo.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbRedo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRedoActionPerformed(evt);
            }
        });
        tlbTop.add(tlbRedo);
        tlbTop.add(spr2);

        tlbRefresh.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/refresh_16.png"))); // NOI18N
        tlbRefresh.setToolTipText(bundle.getString("mnuRefresh")); // NOI18N
        tlbRefresh.setFocusable(false);
        tlbRefresh.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbRefresh.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRefreshActionPerformed(evt);
            }
        });
        tlbTop.add(tlbRefresh);

        tlbFind.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/find_16.png"))); // NOI18N
        tlbFind.setToolTipText(bundle.getString("mnuFind")); // NOI18N
        tlbFind.setFocusable(false);
        tlbFind.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbFind.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbFind.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuFindActionPerformed(evt);
            }
        });
        tlbTop.add(tlbFind);
        tlbTop.add(spr3);

        tlbRun.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/run_16.png"))); // NOI18N
        tlbRun.setToolTipText(bundle.getString("run")); // NOI18N
        tlbRun.setEnabled(false);
        tlbRun.setFocusable(false);
        tlbRun.setHorizontalTextPosition(SwingConstants.CENTER);
        tlbRun.setVerticalTextPosition(SwingConstants.BOTTOM);
        tlbRun.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tlbRunActionPerformed(evt);
            }
        });
        tlbTop.add(tlbRun);

        btnPause.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/pause-16.png"))); // NOI18N
        btnPause.setToolTipText(bundle.getString("pauseResume")); // NOI18N
        btnPause.setFocusable(false);
        btnPause.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPause.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnPauseActionPerformed(evt);
            }
        });
        tlbTop.add(btnPause);

        btnStop.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/stop-16.png"))); // NOI18N
        btnStop.setToolTipText(bundle.getString("stop")); // NOI18N
        btnStop.setFocusable(false);
        btnStop.setHorizontalTextPosition(SwingConstants.CENTER);
        btnStop.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        tlbTop.add(btnStop);
        tlbTop.add(spr4);

        btnReport.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/report_16.png"))); // NOI18N
        btnReport.setToolTipText(bundle.getString("report")); // NOI18N
        btnReport.setFocusable(false);
        btnReport.setHorizontalTextPosition(SwingConstants.CENTER);
        btnReport.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnReportActionPerformed(evt);
            }
        });
        tlbTop.add(btnReport);

        getContentPane().add(tlbTop, BorderLayout.PAGE_START);

        lpnLayer.setLayout(new OverlayLayout(lpnLayer));

        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(204, 204, 204), 2));
        mainPanel.setOpaque(false);
        mainPanel.setPreferredSize(lpnLayer.getPreferredSize());
        lpnLayer.add(mainPanel);

        getContentPane().add(lpnLayer, BorderLayout.CENTER);

        mnuFile.setText(bundle.getString("mnuFile")); // NOI18N

        mnuNew.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/new_16.png"))); // NOI18N
        mnuNew.setText(bundle.getString("mnuNew")); // NOI18N

        mnuNewProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        mnuNewProject.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/project_16.png"))); // NOI18N
        mnuNewProject.setText(bundle.getString("mnuNewProject")); // NOI18N
        mnuNewProject.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuNewProjectActionPerformed(evt);
            }
        });
        mnuNew.add(mnuNewProject);

        mnuRulesFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        mnuRulesFile.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/rules_16.png"))); // NOI18N
        mnuRulesFile.setText(bundle.getString("mnuRulesFile")); // NOI18N
        mnuRulesFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRulesFileActionPerformed(evt);
            }
        });
        mnuNew.add(mnuRulesFile);

        mnuEmptyProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
        mnuEmptyProject.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/project_new_16.png"))); // NOI18N
        mnuEmptyProject.setText(bundle.getString("mnuNewEmptyProject")); // NOI18N
        mnuEmptyProject.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuEmptyProjectActionPerformed(evt);
            }
        });
        mnuNew.add(mnuEmptyProject);

        mnuFile.add(mnuNew);

        mnuOpenProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
        mnuOpenProject.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/open_project_16.png"))); // NOI18N
        mnuOpenProject.setText(bundle.getString("mnuOpenProject")); // NOI18N
        mnuOpenProject.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuOpenProjectActionPerformed(evt);
            }
        });
        mnuFile.add(mnuOpenProject);

        menuRecentProject.setText(bundle.getString("menuOpenRecentProject")); // NOI18N
        mnuFile.add(menuRecentProject);
        mnuFile.add(sprProject);

        mnuImport.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/import_16.png"))); // NOI18N
        mnuImport.setText(bundle.getString("import")); // NOI18N

        mnuImportSoap.setText(bundle.getString("importSoapUI")); // NOI18N
        mnuImportSoap.setEnabled(false);
        mnuImport.add(mnuImportSoap);

        mnuImportPostman.setText(bundle.getString("importPostman")); // NOI18N
        mnuImportPostman.setEnabled(false);
        mnuImport.add(mnuImportPostman);

        mnuFile.add(mnuImport);

        mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
        mnuSave.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/save_icon_16.png"))); // NOI18N
        mnuSave.setText(bundle.getString("mnuSave")); // NOI18N
        mnuSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuSave);

        mnuSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK));
        mnuSaveAs.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/save_as_16.png"))); // NOI18N
        mnuSaveAs.setText(bundle.getString("mnuSaveAs")); // NOI18N
        mnuSaveAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuSaveAsActionPerformed(evt);
            }
        });
        mnuFile.add(mnuSaveAs);
        mnuFile.add(sprExport);

        mnuRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
        mnuRefresh.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/refresh_16.png"))); // NOI18N
        mnuRefresh.setText(bundle.getString("mnuRefresh")); // NOI18N
        mnuRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRefreshActionPerformed(evt);
            }
        });
        mnuFile.add(mnuRefresh);

        mnuPreferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK));
        mnuPreferences.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/setting_16.png"))); // NOI18N
        mnuPreferences.setText(bundle.getString("mnuPreferences")); // NOI18N
        mnuPreferences.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuPreferencesActionPerformed(evt);
            }
        });
        mnuFile.add(mnuPreferences);
        mnuFile.add(sprExit);

        mnuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK));
        mnuExit.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/exit_16.png"))); // NOI18N
        mnuExit.setText(bundle.getString("mnuExit")); // NOI18N
        mnuExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuExitActionPerformed(evt);
            }
        });
        mnuFile.add(mnuExit);

        mnbTop.add(mnuFile);

        mnuEdit.setText(bundle.getString("mnuEdit")); // NOI18N

        mnuUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        mnuUndo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/undo-16.png"))); // NOI18N
        mnuUndo.setText(bundle.getString("mnuUndo")); // NOI18N
        mnuUndo.setEnabled(false);
        mnuUndo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuUndoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuUndo);

        mnuRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        mnuRedo.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/redo-16.png"))); // NOI18N
        mnuRedo.setText(bundle.getString("mnuRedo")); // NOI18N
        mnuRedo.setEnabled(false);
        mnuRedo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRedoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuRedo);
        mnuEdit.add(sprFind);

        mnuFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        mnuFind.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/find_16.png"))); // NOI18N
        mnuFind.setText(bundle.getString("mnuFind")); // NOI18N
        mnuFind.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuFindActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuFind);

        mnbTop.add(mnuEdit);

        mnuProject.setText(bundle.getString("project")); // NOI18N

        mnuRename.setText(bundle.getString("mnuRename")); // NOI18N
        mnuRename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRenameActionPerformed(evt);
            }
        });
        mnuProject.add(mnuRename);

        mnuReload.setText(bundle.getString("mnuReload")); // NOI18N
        mnuReload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuReloadActionPerformed(evt);
            }
        });
        mnuProject.add(mnuReload);

        mnuExport.setText(bundle.getString("mnuExport")); // NOI18N

        mnuSoap.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/export_soapui_16.png"))); // NOI18N
        mnuSoap.setText(bundle.getString("mnuSoap")); // NOI18N
        mnuSoap.setActionCommand("SOAPUI");
        mnuSoap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                export(evt);
            }
        });
        mnuExport.add(mnuSoap);

        mnuReady.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/export_readyapi_16.png"))); // NOI18N
        mnuReady.setText(bundle.getString("mnuReady")); // NOI18N
        mnuReady.setActionCommand("READYAPI");
        mnuReady.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                export(evt);
            }
        });
        mnuExport.add(mnuReady);

        mnuPostman.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/export_postman_16.png"))); // NOI18N
        mnuPostman.setText(bundle.getString("mnuPostman")); // NOI18N
        mnuPostman.setActionCommand("POSTMAN");
        mnuPostman.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                export(evt);
            }
        });
        mnuExport.add(mnuPostman);

        mnuMocha.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/export_mocha_16.png"))); // NOI18N
        mnuMocha.setText(bundle.getString("mnuMocha")); // NOI18N
        mnuMocha.setActionCommand("MOCHA");
        mnuMocha.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                export(evt);
            }
        });
        mnuExport.add(mnuMocha);

        mnuJunit.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/export_junit_16.png"))); // NOI18N
        mnuJunit.setText(bundle.getString("mnuJunit")); // NOI18N
        mnuJunit.setActionCommand("JUNIT");
        mnuJunit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                export(evt);
            }
        });
        mnuExport.add(mnuJunit);

        mnuSwagger.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/swaggers_16.png"))); // NOI18N
        mnuSwagger.setText(bundle.getString("mnuSwagger")); // NOI18N
        mnuSwagger.setActionCommand("SWAGGER");
        mnuSwagger.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                export(evt);
            }
        });
        mnuExport.add(mnuSwagger);
        mnuExport.add(spr6);

        mnuReport.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/report_16.png"))); // NOI18N
        mnuReport.setText(bundle.getString("report")); // NOI18N
        mnuReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuReportActionPerformed(evt);
            }
        });
        mnuExport.add(mnuReport);

        mnuRules.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/rules_16.png"))); // NOI18N
        mnuRules.setText(bundle.getString("mnuRulesFile")); // NOI18N
        mnuRules.setEnabled(false);
        mnuExport.add(mnuRules);

        mnuProject.add(mnuExport);
        mnuProject.add(spr5);

        mnuClose.setText(bundle.getString("mnuCloseProject")); // NOI18N
        mnuClose.setEnabled(false);
        mnuClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuCloseActionPerformed(evt);
            }
        });
        mnuProject.add(mnuClose);

        mnuOpen.setText(bundle.getString("mnuOpenProject")); // NOI18N
        mnuOpen.setEnabled(false);
        mnuOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuOpenActionPerformed(evt);
            }
        });
        mnuProject.add(mnuOpen);

        mnbTop.add(mnuProject);

        mnuView.setText(bundle.getString("mnuView")); // NOI18N

        rbgMnuView.add(mnuUser);
        mnuUser.setSelected(true);
        mnuUser.setText(bundle.getString("mnuUser")); // NOI18N
        mnuUser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuUserActionPerformed(evt);
            }
        });
        mnuView.add(mnuUser);

        rbgMnuView.add(mnuExpert);
        mnuExpert.setText(bundle.getString("mnuExpert")); // NOI18N
        mnuExpert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuExpertActionPerformed(evt);
            }
        });
        mnuView.add(mnuExpert);
        mnuView.add(spr7);

        inheritProp.setSelected(true);
        inheritProp.setText(bundle.getString("inheritedProperties")); // NOI18N
        inheritProp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                inheritPropActionPerformed(evt);
            }
        });
        mnuView.add(inheritProp);
        mnuView.add(spr8);

        mnuProblems.setSelected(true);
        mnuProblems.setText(bundle.getString("problems")); // NOI18N
        mnuProblems.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuProblemsActionPerformed(evt);
            }
        });
        mnuView.add(mnuProblems);

        mnuRaw.setSelected(true);
        mnuRaw.setText(bundle.getString("raw")); // NOI18N
        mnuRaw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuRawActionPerformed(evt);
            }
        });
        mnuView.add(mnuRaw);

        mnuConsole.setSelected(true);
        mnuConsole.setText(bundle.getString("console")); // NOI18N
        mnuConsole.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuConsoleActionPerformed(evt);
            }
        });
        mnuView.add(mnuConsole);

        mnuHistory.setSelected(true);
        mnuHistory.setText(bundle.getString("history")); // NOI18N
        mnuHistory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuHistoryActionPerformed(evt);
            }
        });
        mnuView.add(mnuHistory);

        mnbTop.add(mnuView);

        mnuHelp.setText(bundle.getString("mnuHelp")); // NOI18N

        mnuHelpContent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
        mnuHelpContent.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/help_contents.png"))); // NOI18N
        mnuHelpContent.setText(bundle.getString("mnuHelpContent")); // NOI18N
        mnuHelpContent.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuHelpContentActionPerformed(evt);
            }
        });
        mnuHelp.add(mnuHelpContent);

        mnuAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK));
        mnuAbout.setIcon(new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/about_16.png"))); // NOI18N
        mnuAbout.setText(bundle.getString("mnuAbout")); // NOI18N
        mnuAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mnuAboutActionPerformed(evt);
            }
        });
        mnuHelp.add(mnuAbout);

        mnbTop.add(mnuHelp);

        setJMenuBar(mnbTop);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mnuAboutActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuAboutActionPerformed
        ImageIcon logo = new ImageIcon(getClass().getResource("/com/equinix/amphibia/icons/logo_large.png"));
        ImageIcon resizeIcon = new ImageIcon(logo.getImage().getScaledInstance(40, 40, java.awt.Image.SCALE_DEFAULT));
        JOptionPane.showMessageDialog(this, "Amphibia Lite Edition\n\nVersion: " + VERSION + "\n\nCopyright 2018. All rights reserved.\n\n", bundle.getString("mnuAbout"), JOptionPane.PLAIN_MESSAGE, resizeIcon);
    }//GEN-LAST:event_mnuAboutActionPerformed

    private void mnuHelpContentActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuHelpContentActionPerformed
        helpDialog.openDialog();
    }//GEN-LAST:event_mnuHelpContentActionPerformed

    private void mnuExitActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuExitActionPerformed
        Object[] options = {
            bundle.getString("save"),
            bundle.getString("dontSave"),
            bundle.getString("cancel")
        };
        int option = JOptionPane.showOptionDialog(this,
                bundle.getString("tip_save_changes"),
                bundle.getString("title"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[2]);
        if (option == JOptionPane.NO_OPTION) {
            mainPanel.history.resetHistory(true);
        }
        System.exit(0);
    }//GEN-LAST:event_mnuExitActionPerformed

    private void mnuNewProjectActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuNewProjectActionPerformed
        TreeCollection selectedProject = new TreeCollection();
        selectedProject.setProjectDir(new File("projects", generateTime()));
        openProjectDialog(selectedProject);
    }//GEN-LAST:event_mnuNewProjectActionPerformed

    private void mnuPreferencesActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuPreferencesActionPerformed
        preferenceDialog.openDialog();
    }//GEN-LAST:event_mnuPreferencesActionPerformed

    private void mnuSaveAsActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuSaveAsActionPerformed
        TreeCollection collection = MainPanel.selectedNode.getCollection();
        File file = collection.getProjectFile();
        JFileChooser jc = new JFileChooser();
        jc.setCurrentDirectory(file.getParentFile());
        jc.setSelectedFile(file);
        int rVal = jc.showSaveDialog(null);
        if (rVal != JFileChooser.CANCEL_OPTION) {
            mainPanel.reset(collection);
            try {
                IO.copy(file, jc.getSelectedFile());
                IO.copyDir(new File(file.getParentFile(), "data"), new File(jc.getSelectedFile().getParentFile(), "data"));
                collection.setProjectFile(jc.getSelectedFile());
                mainPanel.loadProject(collection);
            } catch (IOException ex) {
                mainPanel.addError(ex);
            }
        }
    }//GEN-LAST:event_mnuSaveAsActionPerformed

    private void mnuSaveActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuSaveActionPerformed
        mainPanel.history.resetHistory(false);
    }//GEN-LAST:event_mnuSaveActionPerformed

    private void mnuRulesFileActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuRulesFileActionPerformed
        JFileChooser jc = setFileChooserDir(new JFileChooser());
        jc.setFileFilter(new FileNameExtensionFilter("Rule-Properties JSON File", "json", "text"));
        jc.setSelectedFile(new File("rules-properties.json"));
        int rVal = jc.showSaveDialog(null);
        if (rVal != JFileChooser.CANCEL_OPTION) {
            try {
                IO.copy(new File("../resources", "rules_properties_template.json"), jc.getSelectedFile());
                Desktop desktop = Desktop.getDesktop();
                desktop.open(jc.getSelectedFile());
            } catch (IOException ex) {
                mainPanel.addError(ex);
            }
        }
    }//GEN-LAST:event_mnuRulesFileActionPerformed

    private void mnuRefreshActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuRefreshActionPerformed
        mainPanel.reloadAll();
    }//GEN-LAST:event_mnuRefreshActionPerformed

    private void tlbRunActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tlbRunActionPerformed
        if (userPreferences.getBoolean(Amphibia.P_SWITCH_CONSOLE, true)) {
            mainPanel.editor.showHideTab(Amphibia.TAB_CONSOLE, true);
            mainPanel.editor.getTabs().setSelectedIndex(Amphibia.TAB_CONSOLE);
            Amphibia.instance.mnuConsole.setSelected(true);
        }
        if (userPreferences.getBoolean(Amphibia.P_SWITCH_DEBUGGER, true)) {
            mainPanel.tabLeft.setSelectedIndex(1);
        }
        if (userPreferences.getBoolean(Amphibia.P_SHOW_DEBUGGER_TIP, true)) {
            openTipDialog("tip_degguger_mode", P_SHOW_DEBUGGER_TIP);
        }
        mainPanel.runner.runTests();
    }//GEN-LAST:event_tlbRunActionPerformed

    private void mnuOpenProjectActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuOpenProjectActionPerformed
        JFileChooser jf = setFileChooserDir(new JFileChooser());
        jf.setFileFilter(new FileNameExtensionFilter("Amphibia Project", "json", "text"));
        if (MainPanel.selectedNode != null) {
            jf.setCurrentDirectory(MainPanel.selectedNode.getCollection().getProjectDir());
        }
        jf.showOpenDialog(null);
        if (jf.getSelectedFile() != null) {
            saveFileChooserDir(jf);
            String name;
            try {
                JSONObject json = (JSONObject) IO.getJSON(jf.getSelectedFile());
                name = json.getString("name");
            } catch (Exception e) {
                mainPanel.addError(e);
                return;
            }
            Enumeration children = mainPanel.treeNode.children();
            while (children.hasMoreElements()) {
                if (name.equals(((TreeIconNode)children.nextElement()).getLabel())) {
                    JOptionPane.showMessageDialog(mainPanel,
                    bundle.getString("error_project_exists"),
                    bundle.getString("title"),
                    JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            TreeCollection selectedProject = new TreeCollection();
            selectedProject.setProjectFile(jf.getSelectedFile());
            mainPanel.reloadCollection(selectedProject);
            selectedProject.save();
        }
    }//GEN-LAST:event_mnuOpenProjectActionPerformed

    private void mnuUndoActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuUndoActionPerformed
        enableUndo(mainPanel.undo());
        enableRedo(true);
    }//GEN-LAST:event_mnuUndoActionPerformed

    private void mnuRedoActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuRedoActionPerformed
        enableRedo(mainPanel.redo());
        enableUndo(true);
    }//GEN-LAST:event_mnuRedoActionPerformed

    private void mnuFindActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuFindActionPerformed
        findDialog.openDialog();
    }//GEN-LAST:event_mnuFindActionPerformed

    private void mnuUserActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuUserActionPerformed
        userPreferences.putBoolean(P_MENU_VIEW, false);
        isExpertView = false;
        if (MainPanel.selectedNode != null) {
            MainPanel.selectedNode.getCollection().runner.jsonObject().element("expandResources", new JSONObject());
            IO.write(MainPanel.selectedNode.getCollection().runner, mainPanel.editor);
        }
        mainPanel.reloadAll();
    }//GEN-LAST:event_mnuUserActionPerformed

    private void mnuExpertActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuExpertActionPerformed
        userPreferences.putBoolean(P_MENU_VIEW, true);
        isExpertView = true;
        mainPanel.reloadAll();
        if (userPreferences.getBoolean(P_SHOW_EXPERT_TIP, true)) {
            openTipDialog("tip_expert_mode", P_SHOW_EXPERT_TIP);
        }
    }//GEN-LAST:event_mnuExpertActionPerformed

    private void mnuProblemsActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuProblemsActionPerformed
        showHideTab(TAB_PROBLEMS, mnuProblems.isSelected());
    }//GEN-LAST:event_mnuProblemsActionPerformed

    private void mnuConsoleActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuConsoleActionPerformed
        showHideTab(TAB_CONSOLE, mnuConsole.isSelected());
    }//GEN-LAST:event_mnuConsoleActionPerformed

    private void mnuHistoryActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuHistoryActionPerformed
        showHideTab(TAB_HISTORY, mnuHistory.isSelected());
    }//GEN-LAST:event_mnuHistoryActionPerformed

    private void mnuRawActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuRawActionPerformed
        showHideTab(TAB_RAW, mnuRaw.isSelected());
    }//GEN-LAST:event_mnuRawActionPerformed

    private void export(ActionEvent evt) {//GEN-FIRST:event_export
        if (MainPanel.selectedNode != null) {
            export(evt.getActionCommand());
        }
    }//GEN-LAST:event_export

    private void mnuRenameActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuRenameActionPerformed
        if (MainPanel.selectedNode != null) {
            mainPanel.history.renameResource(true, MainPanel.selectedNode.getCollection());
        }
    }//GEN-LAST:event_mnuRenameActionPerformed

    private void mnuReloadActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuReloadActionPerformed
        if (MainPanel.selectedNode != null) {
            mainPanel.reloadCollection(MainPanel.selectedNode.getCollection());
        }
    }//GEN-LAST:event_mnuReloadActionPerformed

    private void mnuCloseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuCloseActionPerformed
        mainPanel.openCloseProject(false);
    }//GEN-LAST:event_mnuCloseActionPerformed

    private void mnuOpenActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuOpenActionPerformed
        mainPanel.openCloseProject(true);
    }//GEN-LAST:event_mnuOpenActionPerformed

    private void btnStopActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        mainPanel.runner.stopTests();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnPauseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnPauseActionPerformed
        mainPanel.runner.pauseResumeTests(btnPause.isSelected());
    }//GEN-LAST:event_btnPauseActionPerformed

    private void mnuReportActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuReportActionPerformed
        btnReportActionPerformed(evt);
    }//GEN-LAST:event_mnuReportActionPerformed

    private void btnReportActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnReportActionPerformed
        try {
            mainPanel.runner.generateJUnitReport();
        } catch (IOException e) {
            mainPanel.addError(e);
        }
    }//GEN-LAST:event_btnReportActionPerformed

    private void inheritPropActionPerformed(ActionEvent evt) {//GEN-FIRST:event_inheritPropActionPerformed
        userPreferences.putBoolean(Amphibia.P_INHERIT_PROPERTIES, inheritProp.isSelected());
        mainPanel.reloadAll();
    }//GEN-LAST:event_inheritPropActionPerformed

    private void mnuEmptyProjectActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mnuEmptyProjectActionPerformed
        int count = mainPanel.treeNode.getChildCount();
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = mainPanel.treeNode.getChildAt(i).toString();
        }
        String name = inputDialog("projectName", "", names);
        if (name != null) {
            JFileChooser jc = setFileChooserDir(new JFileChooser());
            jc.setFileFilter(new FileNameExtensionFilter("Amphibia Project File", "json", "text"));
            jc.setSelectedFile(new File(name + ".json"));
            int rVal = jc.showSaveDialog(null);
            if (rVal != JFileChooser.CANCEL_OPTION) {
                try {
                    String content = IO.readFile(new File("../resources", "project_template.json"));
                    IO.write(content.replace("<% PROJECT_NAME %>", name), jc.getSelectedFile());
                    File dataDir = new File(jc.getSelectedFile().getParentFile(), "data");
                    if (dataDir.mkdirs()) {
                        IO.copy(new File("../resources", "runner_template.json"), new File(dataDir, "runner.json"));
                    }
                    TreeCollection selectedProject = new TreeCollection();
                    selectedProject.setProjectFile(jc.getSelectedFile());
                    mainPanel.loadProject(selectedProject);
                    mainPanel.expandDefaultNodes(selectedProject);
                    selectedProject.save();
                    if (mainPanel.tabRight.isEnabledAt(1)) {
                        mainPanel.tabRight.setSelectedIndex(1);
                    }
                } catch (IOException ex) {
                    mainPanel.addError(ex);
                }
            }
        }
    }//GEN-LAST:event_mnuEmptyProjectActionPerformed

    public void export(String type) {
        Amphibia.setWaitOverlay(true);
        new Thread() {
            @Override
            public void run() {
                final TreeCollection selectedProject = MainPanel.selectedNode.getCollection();
                File file = selectedProject.getProjectFile();
                try {
                    String[] args = new String[]{
                        "-i=" + file.getAbsolutePath(),
                        "-f=" + type,
                        "groupId=" + preferenceDialog.txtGroupId.getText(),
                        "-j=true", "-r=true"
                    };
                    logger.log(Level.INFO, String.join(" ", args));
                    Map<String, Object> results = com.equinix.amphibia.agent.builder.Builder.execute(args);
                    logger.log(Level.INFO, String.valueOf(results));
                    Desktop desktop = Desktop.getDesktop();
                    File projectFile = new File(selectedProject.getProjectDir().getParentFile(), results.get("project").toString());
                    desktop.open(projectFile.getParentFile());
                } catch (Exception e) {
                    mainPanel.addError(e);
                }
                Amphibia.setWaitOverlay(false);
            }
        }.start();
    }

    public static String generateTime() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    }

    public static void setBounds(Component comp, String name) {
        Rectangle r = comp.getBounds();
        int[] data = new int[]{r.x, r.y, r.width, r.height};
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
        byteBuffer.asIntBuffer().put(data);
        getUserPreferences().putByteArray(name, byteBuffer.array());
    }

    public static void getBounds(Component comp, String name) {
        byte[] bytes = getUserPreferences().getByteArray(name, null);
        if (bytes != null) {
            IntBuffer intBuf = ByteBuffer.wrap(bytes).asIntBuffer();
            int[] array = new int[intBuf.remaining()];
            intBuf.get(array);
            Rectangle r = new Rectangle(array[0], array[1], array[2], array[3]);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (GraphicsDevice gd : ge.getScreenDevices()) {
                Rectangle b = gd.getDefaultConfiguration().getBounds();
                if (r.x >= b.x && r.x < b.x + b.width && r.y >= b.y && r.y < b.y + b.height) {
                    comp.setBounds(r);
                    return;
                }
            }
        }
    }

    public static JFileChooser setFileChooserDir(JFileChooser jf) {
        Preferences pref = getUserPreferences();
        File file = new File(pref.get(P_LAST_DIRECTORY, "."));
        if (file.exists()) {
            jf.setCurrentDirectory(file);
        }
        return jf;
    }

    public static void saveFileChooserDir(JFileChooser jf) {
        getUserPreferences().put(P_LAST_DIRECTORY, jf.getSelectedFile().getParentFile().getAbsolutePath());
    }

    public String inputDialog(String bundleKey, String initValue, String[] exisitingName) {
        final JOptionPane optionPane = new JOptionPane(
                "", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        optionPane.setWantsInput(true);
        optionPane.setInitialSelectionValue(initValue);
        
        final JLabel error = new JLabel(bundle.getString("tip_name_exists"), JLabel.LEFT);
        error.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 75));
        error.setForeground(Color.red);
        optionPane.add(error, 1);

        final JDialog dialog = new JDialog(this, bundle.getString(bundleKey), true);
        dialog.setContentPane(optionPane);
        optionPane.addPropertyChangeListener((PropertyChangeEvent e) -> {
            if (optionPane.getValue().equals(JOptionPane.OK_OPTION)) {
                error.setVisible(false);
                for (String name : exisitingName) {
                    if (name.equals(optionPane.getInputValue())) {
                        error.setVisible(true);
                        return;
                    }
                }
                dialog.setVisible(false);
            } else if (optionPane.getValue().equals(JOptionPane.CANCEL_OPTION)) {
                dialog.setVisible(false);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        error.setVisible(false);
        dialog.setVisible(true);
        dialog.dispose();
        return !error.isVisible() && optionPane.getValue().equals(JOptionPane.OK_OPTION) ? optionPane.getInputValue().toString() : null;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnCreate;
    public JToggleButton btnPause;
    private JButton btnReport;
    private JButton btnStop;
    private JCheckBoxMenuItem inheritProp;
    private JLabel lblAnimation;
    private JLayeredPane lpnLayer;
    private MainPanel mainPanel;
    private JMenu menuRecentProject;
    private JMenuBar mnbTop;
    private JMenuItem mnuAbout;
    public JMenuItem mnuClose;
    public JCheckBoxMenuItem mnuConsole;
    private JMenu mnuEdit;
    private JMenuItem mnuEmptyProject;
    private JMenuItem mnuExit;
    private JRadioButtonMenuItem mnuExpert;
    private JMenu mnuExport;
    private JMenu mnuFile;
    private JMenuItem mnuFind;
    private JMenu mnuHelp;
    private JMenuItem mnuHelpContent;
    private JCheckBoxMenuItem mnuHistory;
    private JMenu mnuImport;
    private JMenuItem mnuImportPostman;
    private JMenuItem mnuImportSoap;
    private JMenuItem mnuJunit;
    private JMenuItem mnuMocha;
    private JMenu mnuNew;
    private JMenuItem mnuNewProject;
    public JMenuItem mnuOpen;
    private JMenuItem mnuOpenProject;
    private JMenuItem mnuPostman;
    private JMenuItem mnuPreferences;
    public JCheckBoxMenuItem mnuProblems;
    private JMenu mnuProject;
    private JCheckBoxMenuItem mnuRaw;
    private JMenuItem mnuReady;
    private JMenuItem mnuRedo;
    private JMenuItem mnuRefresh;
    private JMenuItem mnuReload;
    private JMenuItem mnuRename;
    private JMenuItem mnuReport;
    private JMenuItem mnuRules;
    private JMenuItem mnuRulesFile;
    private JMenuItem mnuSave;
    private JMenuItem mnuSaveAs;
    private JMenuItem mnuSoap;
    private JMenuItem mnuSwagger;
    private JMenuItem mnuUndo;
    private JRadioButtonMenuItem mnuUser;
    private JMenu mnuView;
    private JPanel pnlWaitOverlay;
    private ButtonGroup rbgMnuView;
    private JToolBar.Separator spr1;
    private JToolBar.Separator spr2;
    private JToolBar.Separator spr3;
    private JToolBar.Separator spr4;
    private JPopupMenu.Separator spr5;
    private JPopupMenu.Separator spr6;
    private JPopupMenu.Separator spr7;
    private JPopupMenu.Separator spr8;
    private JPopupMenu.Separator sprExit;
    private JPopupMenu.Separator sprExport;
    private JPopupMenu.Separator sprFind;
    private JPopupMenu.Separator sprProject;
    private JButton tlbFind;
    private JButton tlbOpen;
    private JButton tlbRedo;
    private JButton tlbRefresh;
    public JButton tlbRun;
    private JButton tlbSave;
    private JToolBar tlbTop;
    private JButton tlbUndo;
    // End of variables declaration//GEN-END:variables
}
